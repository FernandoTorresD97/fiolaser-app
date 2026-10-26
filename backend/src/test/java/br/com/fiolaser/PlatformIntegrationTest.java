package br.com.fiolaser;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.fiolaser.dto.*;
import br.com.fiolaser.model.*;
import br.com.fiolaser.notification.NotificationConsumer;
import br.com.fiolaser.repository.*;
import br.com.fiolaser.security.*;
import br.com.fiolaser.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    properties = {
      "app.outbox.enabled=false",
      "spring.kafka.admin.auto-create=false",
      "spring.kafka.listener.auto-startup=false",
      "app.notifications.enabled=true",
      "app.security.cpf.pepper=test-only-pepper",
      "app.security.cpf.aes-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
      "ADMIN_BOOTSTRAP_EMAIL=",
      "ADMIN_BOOTSTRAP_PASSWORD="
    })
@AutoConfigureMockMvc
class PlatformIntegrationTest {
  static final io.zonky.test.db.postgres.embedded.EmbeddedPostgres postgres = database();
  static io.zonky.test.db.postgres.embedded.EmbeddedPostgres database() {
    try { return io.zonky.test.db.postgres.embedded.EmbeddedPostgres.builder().setPort(0).start(); }
    catch(java.io.IOException ex) { throw new ExceptionInInitializerError(ex); }
  }
  @AfterAll static void closeDatabase() throws java.io.IOException { postgres.close(); }

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres","postgres"));
    r.add("spring.datasource.username", () -> "postgres");
    r.add("spring.datasource.password", () -> "postgres");
  }

  @Autowired UserRepository users;
  @Autowired EstheticServiceRepository services;
  @Autowired BookingRepository bookings;
  @Autowired AuthService auth;
  @Autowired BookingService agenda;
  @Autowired AdminUserService admins;
  @Autowired OAuthCodeService codes;
  @Autowired JwtService jwt;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired PlatformTransactionManager transactions;
  @Autowired NotificationConsumer notifications;
  @MockBean JavaMailSender mail;
  User customer;
  EstheticService treatment;
  LocalDateTime start;

  @BeforeEach
  void setup() {
    jdbc.update("DELETE FROM auth_attempt_buckets");
    jdbc.update("DELETE FROM notification_deliveries");
    jdbc.update("DELETE FROM booking_outbox");
    jdbc.update("DELETE FROM oauth_login_codes");
    bookings.deleteAll();
    users.deleteAll();
    customer =
        users.save(
            User.builder()
                .nomeCompleto("Cliente teste")
                .email("cliente@example.test")
                .cpfHash("test-cpf")
                .cpfEncrypted("encrypted")
                .dataNascimento(LocalDate.of(1990, 1, 1))
                .provider(AuthProvider.LOCAL)
                .role(Role.CLIENTE)
                .ativo(true)
                .senhaHash(encoder.encode("SenhaTeste123!"))
                .build());
    treatment = services.findAll().getFirst();
    treatment.setDuracaoMinutos(60);
    treatment.setAtivo(true);
    services.save(treatment);
    start = LocalDate.now(ZoneId.of("America/Bahia")).plusDays(3).atTime(10, 0);
  }

  BookingRequest request(LocalDateTime time) {
    return new BookingRequest(treatment.getId(), time, PaymentMethod.PIX);
  }

  String token(User u) {
    return jwt.gerarToken(u.getId(), u.getRole().name(), u.getTokenVersion());
  }

  @Test
  void canceledSlotCanBeBookedAgain() {
    var first = agenda.criar(customer.getId(), request(start));
    agenda.cancelar(customer.getId(), first.id());
    var second = agenda.criar(customer.getId(), request(start));
    assertThat(second.id()).isNotEqualTo(first.id());
    assertThat(bookings.count()).isEqualTo(2);
  }

  @Test
  void durationBlocksOverlappingSlotAndClosingTime() {
    agenda.criar(customer.getId(), request(start));
    assertThatThrownBy(() -> agenda.criar(customer.getId(), request(start.plusMinutes(30))))
        .isInstanceOf(IllegalStateException.class);
    assertThat(agenda.disponibilidadeDoDia(treatment.getId(), start.toLocalDate()))
        .filteredOn(s -> s.horario().equals(start.plusMinutes(30)))
        .allMatch(s -> !s.disponivel());
    assertThatThrownBy(
            () -> agenda.criar(customer.getId(), request(start.withHour(18).withMinute(30))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonGridAndInactiveService() {
    assertThatThrownBy(() -> agenda.criar(customer.getId(), request(start.withMinute(17))))
        .isInstanceOf(IllegalArgumentException.class);
    treatment.setAtivo(false);
    services.save(treatment);
    assertThatThrownBy(() -> agenda.criar(customer.getId(), request(start)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void incompleteGoogleUserCannotBook() {
    var u = auth.loginOuRegistrarComGoogle("google-sub", "google@example.test", "Google teste");
    assertThat(u.perfilCompleto()).isFalse();
    assertThatThrownBy(() -> agenda.criar(u.getId(), request(start)))
        .hasMessageContaining("Complete");
    assertThat(
            auth.completar(
                    u.getId(),
                    new CompleteProfileRequest("52998224725", LocalDate.of(1990, 1, 1), null))
                .perfilCompleto())
        .isTrue();
    assertThat(agenda.criar(u.getId(), request(start))).isNotNull();
  }

  @Test
  void googleDoesNotSilentlyLinkLocalAccount() {
    assertThatThrownBy(() -> auth.loginOuRegistrarComGoogle("sub", customer.getEmail(), "Teste"))
        .hasMessageContaining("senha");
  }

  @Test
  void oauthCodeIsSingleUseAndExpires() {
    String code = codes.emitir(customer.getId());
    assertThat(codes.trocar(code).token()).isNotBlank();
    assertThatThrownBy(() -> codes.trocar(code)).isInstanceOf(IllegalArgumentException.class);
    String expired = codes.emitir(customer.getId());
    jdbc.update("UPDATE oauth_login_codes SET expires_at=now()-interval '1 minute'");
    assertThatThrownBy(() -> codes.trocar(expired)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void passwordChangeRevokesOldToken() throws Exception {
    String before = token(customer);
    var response =
        auth.trocarSenha(
            customer.getId(), new ChangePasswordRequest("SenhaTeste123!", "NovaSenha123!"));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + before))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + response.token()))
        .andExpect(status().isOk());
  }

  @Test
  void disabledUserLosesAccessAndCannotLogin() throws Exception {
    String before = token(customer);
    customer.setAtivo(false);
    users.save(customer);
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + before))
        .andExpect(status().isUnauthorized());
    assertThatThrownBy(() -> auth.login(new LoginRequest(customer.getEmail(), "SenhaTeste123!")))
        .hasMessageContaining("Credenciais");
  }

  @Test
  void adminEndpointsRejectCustomersAndAnonymous() throws Exception {
    mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token(customer)))
        .andExpect(status().isForbidden());
    mvc.perform(put("/api/auth/perfil").contentType("application/json").content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void concurrentReservationsHaveOneWinner() throws Exception {
    var gate = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> task =
          () -> {
            gate.await();
            try {
              agenda.criar(customer.getId(), request(start));
              return true;
            } catch (IllegalStateException e) {
              return false;
            }
          };
      var one = executor.submit(task);
      var two = executor.submit(task);
      gate.countDown();
      assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(bookings.count()).isEqualTo(1);
  }

  @Test
  void bookingAndOutboxRollbackTogether() {
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactions)
                    .execute(
                        status -> {
                          agenda.criar(customer.getId(), request(start));
                          throw new IllegalStateException("rollback");
                        }))
        .hasMessage("rollback");
    assertThat(bookings.count()).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM booking_outbox", Long.class)).isZero();
  }

  @Test
  void notificationDuplicateSendsOneEmail() throws Exception {
    agenda.criar(customer.getId(), request(start));
    String payload = jdbc.queryForObject("SELECT payload FROM booking_outbox", String.class);
    notifications.receive(payload);
    notifications.receive(payload);
    verify(mail, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));
  }

  @Test
  void mailFailureDoesNotMarkDelivery() {
    agenda.criar(customer.getId(), request(start));
    String payload = jdbc.queryForObject("SELECT payload FROM booking_outbox", String.class);
    doThrow(new org.springframework.mail.MailSendException("offline"))
        .when(mail)
        .send(any(org.springframework.mail.SimpleMailMessage.class));
    assertThatThrownBy(() -> notifications.receive(payload))
        .isInstanceOf(org.springframework.mail.MailSendException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_deliveries", Long.class))
        .isZero();
  }

  @Test
  void adminEditMovesSlotAndDateRangeWorks() {
    var b = agenda.criar(customer.getId(), request(start));
    agenda.editarComoAdmin(
        b.id(),
        new AdminBookingRequest(
            customer.getId(), treatment.getId(), start.plusHours(2), PaymentMethod.CARTAO));
    assertThat(agenda.listarParaAdmin(start.toLocalDate(), start.toLocalDate()))
        .singleElement()
        .satisfies(a -> assertThat(a.horario()).isEqualTo(start.plusHours(2)));
    assertThat(agenda.criar(customer.getId(), request(start))).isNotNull();
  }

  @Test
  void cannotRemoveOwnAdminAccess() {
    customer.setRole(Role.ADMIN);
    users.save(customer);
    assertThatThrownBy(
            () ->
                admins.atualizar(
                    customer.getId(),
                    customer.getId(),
                    new AdminUserService.Update(Role.CLIENTE, false)))
        .hasMessageContaining("próprio");
  }

  @Test
  void nullBirthDateGetsValidationError() throws Exception {
    mvc.perform(
            post("/api/auth/registrar")
                .contentType("application/json")
                .content(
                    """
   {"nomeCompleto":"Teste","email":"new@example.test","cpf":"52998224725","senha":"SenhaTeste123!"}
   """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.dataNascimento").exists());
  }

  @Test
  void paymentsAreExplicitlyUnavailable() throws Exception {
    mvc.perform(post("/api/payments/checkout").header("Authorization", "Bearer " + token(customer)))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void unknownEmailReturnsGenericCredentialsError() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    "{\"identificador\":\"unknown@example.test\",\"senha\":\"SenhaTeste123!\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.erro").value("Credenciais inválidas."));
  }

  @Test void logoutRevokesPreviousToken() throws Exception {
    String access=token(customer);
    mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+access)).andExpect(status().isNoContent());
    mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+access)).andExpect(status().isUnauthorized());
  }
  @Test void logoutRequiresAuthentication() throws Exception {mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());}
  @Test void failedLoginsAreThrottledAcrossAddressesByIdentifier() throws Exception {
    String body="{\"identificador\":\"nobody@example.test\",\"senha\":\"wrong-test-password\"}";
    for(int i=0;i<10;i++)mvc.perform(post("/api/auth/login").with(r->{r.setRemoteAddr("192.0.2."+java.util.concurrent.ThreadLocalRandom.current().nextInt(1,250));return r;}).contentType("application/json").content(body)).andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/login").contentType("application/json").content(body)).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    assertThat(jdbc.queryForList("select bucket_key from auth_attempt_buckets",String.class)).allMatch(key->key.matches("[a-f0-9]{64}"));
  }
  @Test void authenticationIpLimitCannotBeBypassedByChangingIdentifier() throws Exception {
    var limiter=new AuthAttemptLimiter(jdbc,java.time.Clock.fixed(java.time.Instant.parse("2030-01-01T12:00:00Z"),java.time.ZoneOffset.UTC),"test-limiter-key");
    for(int i=0;i<30;i++)limiter.check("login","192.0.2.1","user"+i+"@example.test");
    assertThatThrownBy(()->limiter.check("login","192.0.2.1","different@example.test")).isInstanceOf(AuthThrottledException.class);
  }
  @Test void authenticationLimitExpiresAtNextWindow() {
    var first=new AuthAttemptLimiter(jdbc,java.time.Clock.fixed(java.time.Instant.parse("2030-01-01T12:00:00Z"),java.time.ZoneOffset.UTC),"test-limiter-key");
    for(int i=0;i<10;i++)first.check("login","192.0.2.1","same@example.test");
    assertThatThrownBy(()->first.check("login","192.0.2.1","same@example.test")).isInstanceOf(AuthThrottledException.class);
    var later=new AuthAttemptLimiter(jdbc,java.time.Clock.fixed(java.time.Instant.parse("2030-01-01T12:05:00Z"),java.time.ZoneOffset.UTC),"test-limiter-key");
    assertThatCode(()->later.check("login","192.0.2.1","same@example.test")).doesNotThrowAnyException();
  }

  @Test
  void disallowOutsideBusinessHoursAndPast() {
    assertThatThrownBy(() -> agenda.criar(customer.getId(), request(start.withHour(3))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> agenda.criar(customer.getId(), request(start.minusDays(10))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void authorizedCsvIncludesQuotedCustomerAndCorrectHeaders() throws Exception {
    customer.setRole(Role.ADMIN);
    customer.setNomeCompleto("=FORMULA");
    users.save(customer);
    agenda.criar(customer.getId(), request(start));
    mvc.perform(
            get("/api/admin/bookings/export")
                .param("inicio", start.toLocalDate().toString())
                .param("fim", start.toLocalDate().toString())
                .header("Authorization", "Bearer " + token(customer)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=agendamentos.csv"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("'=FORMULA")));
  }

  @Test
  void notificationWithoutEmailUsesSmsAdapter() throws Exception {
    customer.setEmail(null);
    customer.setTelefone("75999990000");
    users.save(customer);
    agenda.criar(customer.getId(), request(start));
    String payload = jdbc.queryForObject("SELECT payload FROM booking_outbox", String.class);
    var provider = new org.springframework.beans.factory.support.StaticListableBeanFactory();
    var sms = mock(br.com.fiolaser.notification.SmsSender.class);
    provider.addBean("sms", sms);
    var consumer =
        new NotificationConsumer(
            json,
            jdbc,
            mail,
            transactions,
            "test@localhost",
            provider.getBeanProvider(br.com.fiolaser.notification.SmsSender.class));
    consumer.receive(payload);
    consumer.receive(payload);
    verify(sms, times(1)).send(eq("75999990000"), contains("reserva recebida"));
    verifyNoInteractions(mail);
  }
}
