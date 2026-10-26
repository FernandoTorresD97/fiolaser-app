package br.com.fiolaser.service;

import br.com.fiolaser.dto.*;
import br.com.fiolaser.kafka.BookingEventProducer;
import br.com.fiolaser.kafka.events.BookingCreatedEvent;
import br.com.fiolaser.model.*;
import br.com.fiolaser.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookingService {
  private static final LocalTime OPEN = LocalTime.of(9, 0), CLOSE = LocalTime.of(19, 0);
  private static final List<BookingStatus> ACTIVE =
      List.of(BookingStatus.RESERVADO, BookingStatus.CONFIRMADO);
  private final BookingRepository bookings;
  private final EstheticServiceRepository services;
  private final UserRepository users;
  private final BookingEventProducer events;
  private final Clock clock;

  public BookingService(
      BookingRepository bookings,
      EstheticServiceRepository services,
      UserRepository users,
      BookingEventProducer events,
      Clock clock) {
    this.bookings = bookings;
    this.services = services;
    this.users = users;
    this.events = events;
    this.clock = clock;
  }

  public List<SlotResponse> disponibilidadeDoDia(String id, LocalDate day) {
    var s =
        services
            .findById(id)
            .filter(EstheticService::isAtivo)
            .orElseThrow(() -> new IllegalArgumentException("Serviço indisponível."));
    var occupied = bookings.overlaps(id, day.atTime(OPEN), day.atTime(CLOSE), ACTIVE);
    var slots = new ArrayList<SlotResponse>();
    for (var t = day.atTime(OPEN);
        !t.plusMinutes(s.getDuracaoMinutos()).isAfter(day.atTime(CLOSE));
        t = t.plusMinutes(30)) {
      var start = t;
      var end = t.plusMinutes(s.getDuracaoMinutos());
      slots.add(
          new SlotResponse(
              t,
              t.isAfter(LocalDateTime.now(clock))
                  && occupied.stream()
                      .noneMatch(
                          b -> start.isBefore(b.getFimHorario()) && end.isAfter(b.getHorario()))));
    }
    return slots;
  }

  private User user(String id) {
    var u =
        users
            .findById(id)
            .filter(User::isAtivo)
            .orElseThrow(() -> new IllegalArgumentException("Usuário indisponível."));
    if (!u.perfilCompleto())
      throw new IllegalArgumentException("Complete CPF e data de nascimento antes de agendar.");
    return u;
  }

  private EstheticService validate(BookingRequest req, String ignoredId) {
    var s =
        services
            .findLockedById(req.serviceId())
            .filter(EstheticService::isAtivo)
            .orElseThrow(() -> new IllegalArgumentException("Serviço indisponível."));
    var start = req.horario();
    var end = start.plusMinutes(s.getDuracaoMinutos());
    if (!start.isAfter(LocalDateTime.now(clock))
        || start.toLocalTime().isBefore(OPEN)
        || end.isAfter(start.toLocalDate().atTime(CLOSE))
        || start.getMinute() % 30 != 0
        || start.getSecond() != 0
        || start.getNano() != 0)
      throw new IllegalArgumentException(
          "Escolha um horário futuro, a cada 30 minutos, entre 09h e 19h, incluindo a duração do serviço.");
    if (bookings.overlaps(s.getId(), start, end, ACTIVE).stream()
        .anyMatch(b -> !b.getId().equals(ignoredId)))
      throw new IllegalStateException("Horário ocupado. Escolha outro.");
    return s;
  }

  @Transactional
  public BookingResponse criar(String userId, BookingRequest req) {
    return response(create(userId, req));
  }

  private Booking create(String userId, BookingRequest req) {
    var u = user(userId);
    var s = validate(req, null);
    var b =
        bookings.save(
            Booking.builder()
                .user(u)
                .service(s)
                .horario(req.horario())
                .fimHorario(req.horario().plusMinutes(s.getDuracaoMinutos()))
                .valor(s.getPreco())
                .formaPagamento(req.formaPagamento())
                .status(BookingStatus.RESERVADO)
                .build());
    events.publicarAgendamentoCriado(
        new BookingCreatedEvent(
            UUID.randomUUID().toString(),
            b.getId(),
            u.getId(),
            u.getNomeCompleto(),
            u.getEmail(),
            u.getTelefone(),
            s.getNome(),
            b.getHorario(),
            b.getFormaPagamento().name()));
    return b;
  }

  public List<BookingResponse> meusAgendamentos(String id) {
    return bookings.findByUserIdOrderByHorarioDesc(id).stream().map(this::response).toList();
  }

  @Transactional
  public void cancelar(String userId, String id) {
    var b = locked(id);
    if (!b.getUser().getId().equals(userId))
      throw new org.springframework.security.access.AccessDeniedException("Acesso negado.");
    cancel(b);
  }

  private Booking locked(String id) {
    return bookings
        .findLockedById(id)
        .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
  }

  private void cancel(Booking b) {
    if (b.getStatus() == BookingStatus.CANCELADO) return;
    if (b.getStatus() == BookingStatus.CONCLUIDO)
      throw new IllegalStateException("Agendamento já concluído.");
    b.setStatus(BookingStatus.CANCELADO);
  }

  public List<AdminBookingResponse> listarParaAdmin(LocalDate inicio, LocalDate fim) {
    LocalDate from = inicio == null ? LocalDate.now(clock).withDayOfMonth(1) : inicio;
    LocalDate to = fim == null ? from.plusMonths(1).minusDays(1) : fim;
    if (to.isBefore(from) || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 366)
      throw new IllegalArgumentException("Informe um período de até 366 dias.");
    return bookings
        .findByHorarioGreaterThanEqualAndHorarioLessThanOrderByHorarioAsc(
            from.atStartOfDay(), to.plusDays(1).atStartOfDay())
        .stream()
        .map(this::admin)
        .toList();
  }

  @Transactional
  public AdminBookingResponse criarComoAdmin(AdminBookingRequest req) {
    return admin(create(req.userId(), req.booking()));
  }

  @Transactional
  public AdminBookingResponse editarComoAdmin(String id, AdminBookingRequest req) {
    var b = locked(id);
    if (!ACTIVE.contains(b.getStatus()))
      throw new IllegalStateException("Somente reservas ativas podem ser editadas.");
    var u = user(req.userId());
    var s = validate(req.booking(), id);
    b.setUser(u);
    b.setService(s);
    b.setHorario(req.horario());
    b.setFimHorario(req.horario().plusMinutes(s.getDuracaoMinutos()));
    b.setValor(s.getPreco());
    b.setFormaPagamento(req.formaPagamento());
    return admin(b);
  }

  @Transactional
  public void cancelarComoAdmin(String id) {
    cancel(locked(id));
  }

  private BookingResponse response(Booking b) {
    return new BookingResponse(
        b.getId(), b.getService().getNome(), b.getHorario(), b.getFormaPagamento(), b.getStatus());
  }

  private AdminBookingResponse admin(Booking b) {
    return new AdminBookingResponse(
        b.getId(),
        b.getUser().getNomeCompleto(),
        b.getUser().getEmail(),
        b.getUser().getTelefone(),
        b.getService().getNome(),
        b.getHorario(),
        b.getFormaPagamento(),
        b.getStatus(),
        b.getUser().getId(),
        b.getService().getId(),
        b.getFimHorario(),
        b.getValor());
  }
}
