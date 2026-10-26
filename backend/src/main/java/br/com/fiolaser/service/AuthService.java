package br.com.fiolaser.service;

import br.com.fiolaser.dto.*;
import br.com.fiolaser.model.*;
import br.com.fiolaser.repository.UserRepository;
import br.com.fiolaser.security.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final CpfProtector cpf;
  private final String unknownPasswordHash;

  public AuthService(
      UserRepository users, PasswordEncoder passwords, JwtService jwt, CpfProtector cpf) {
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
    this.cpf = cpf;
    this.unknownPasswordHash = passwords.encode(java.util.UUID.randomUUID().toString());
  }

  public static String email(String value) {
    return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  public static String telefone(String value) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.replaceAll("[^0-9+]", "");
    if (!normalized.matches("\\+?[0-9]{10,15}"))
      throw new IllegalArgumentException("Telefone inválido.");
    return normalized;
  }

  public static void validarSenha(String value) {
    if (value == null || value.length() < 10 || value.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "A senha deve ter ao menos 10 caracteres e no máximo 72 bytes.");
  }

  @Transactional
  public AuthResponse registrar(RegisterRequest req) {
    String email = email(req.email()), telefone = telefone(req.telefone());
    if (email == null && telefone == null)
      throw new IllegalArgumentException("Informe e-mail ou telefone.");
    validarSenha(req.senha());
    if (!cpf.cpfValido(req.cpf())) throw new IllegalArgumentException("CPF inválido.");
    if (users.existsByCpfHash(cpf.hash(req.cpf())))
      throw new IllegalArgumentException("CPF já cadastrado.");
    if (email != null && users.findByEmailIgnoreCase(email).isPresent())
      throw new IllegalArgumentException("E-mail já cadastrado.");
    if (telefone != null && users.existsByTelefone(telefone))
      throw new IllegalArgumentException("Telefone já cadastrado.");
    User user =
        User.builder()
            .nomeCompleto(req.nomeCompleto().trim())
            .email(email)
            .telefone(telefone)
            .cpfHash(cpf.hash(req.cpf()))
            .cpfEncrypted(cpf.encrypt(req.cpf()))
            .dataNascimento(req.dataNascimento())
            .senhaHash(passwords.encode(req.senha()))
            .provider(AuthProvider.LOCAL)
            .role(Role.CLIENTE)
            .ativo(true)
            .build();
    return resposta(users.save(user));
  }

  public AuthResponse login(LoginRequest req) {
    User user =
        users
            .findByEmailIgnoreCase(req.identificador().trim())
            .or(() -> users.findByTelefone(req.identificador().trim()))
            .or(
                () ->
                    req.identificador().matches("[+0-9 ()-]{10,20}")
                        ? users.findByTelefone(telefone(req.identificador()))
                        : java.util.Optional.empty())
            .orElse(null);
    if(user==null || !user.isAtivo() || user.getSenhaHash()==null) {
      passwords.matches(req.senha(),unknownPasswordHash);
      throw new IllegalArgumentException("Credenciais inválidas.");
    }
    if (!user.isAtivo()
        || user.getSenhaHash() == null
        || !passwords.matches(req.senha(), user.getSenhaHash()))
      throw new IllegalArgumentException("Credenciais inválidas.");
    return resposta(user);
  }

  @Transactional
  public User loginOuRegistrarComGoogle(String subject, String email, String nome) {
    if (subject == null || subject.isBlank() || email(email) == null)
      throw new IllegalArgumentException("Identidade Google inválida.");
    var existing = users.findByProviderAndProviderId(AuthProvider.GOOGLE, subject);
    if (existing.isPresent()) {
      if (!existing.get().isAtivo()) throw new IllegalArgumentException("Conta desativada.");
      return existing.get();
    }
    if (users.findByEmailIgnoreCase(email(email)).isPresent())
      throw new IllegalArgumentException("Este e-mail já tem uma conta. Entre com sua senha.");
    return users.save(
        User.builder()
            .nomeCompleto(nome == null ? email : nome)
            .email(email(email))
            .provider(AuthProvider.GOOGLE)
            .providerId(subject)
            .role(Role.CLIENTE)
            .ativo(true)
            .build());
  }

  public User usuario(String id) {
    return users
        .findById(id)
        .filter(User::isAtivo)
        .orElseThrow(() -> new IllegalArgumentException("Usuário indisponível."));
  }

  public AuthResponse resposta(User user) {
    return new AuthResponse(
        jwt.gerarToken(user.getId(), user.getRole().name(), user.getTokenVersion()),
        user.getNomeCompleto(),
        user.getRole().name(),
        user.perfilCompleto());
  }

  @Transactional
  public AuthResponse completar(String id, CompleteProfileRequest req) {
    User user = users.findLockedById(id).orElseThrow();
    if (user.perfilCompleto()) throw new IllegalStateException("Cadastro já completo.");
    if (!cpf.cpfValido(req.cpf())) throw new IllegalArgumentException("CPF inválido.");
    String hash = cpf.hash(req.cpf());
    if (users.existsByCpfHash(hash)) throw new IllegalArgumentException("CPF já cadastrado.");
    String phone = telefone(req.telefone());
    if (phone != null && users.existsByTelefone(phone))
      throw new IllegalArgumentException("Telefone já cadastrado.");
    user.setCpfHash(hash);
    user.setCpfEncrypted(cpf.encrypt(req.cpf()));
    user.setDataNascimento(req.dataNascimento());
    if (phone != null) user.setTelefone(phone);
    return resposta(user);
  }

  @Transactional
  public AuthResponse trocarSenha(String id, ChangePasswordRequest req) {
    User user = users.findLockedById(id).orElseThrow();
    if (user.getSenhaHash() == null)
      throw new IllegalArgumentException("Sua senha é gerenciada pelo Google.");
    if (!passwords.matches(req.senhaAtual(), user.getSenhaHash()))
      throw new IllegalArgumentException("Senha atual incorreta.");
    validarSenha(req.novaSenha());
    user.setSenhaHash(passwords.encode(req.novaSenha()));
    user.setTokenVersion(user.getTokenVersion() + 1);
    return resposta(user);
  }

  @Transactional
  public void revogarAcessos(String id) {
    User user=users.findLockedById(id).orElseThrow();
    user.setTokenVersion(user.getTokenVersion()+1);
  }
}
