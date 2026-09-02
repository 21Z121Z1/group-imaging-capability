package com.graduation.gym.auth;

import com.graduation.gym.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {
  private static final String DUMMY_BCRYPT = "$2a$12$7EqJtq98hPqEX7fNZaFWoO5eV3L9xRtJ8VN5KW7LkM5JcAQx6h8jK";
  private final UserRepository users;
  private final AuthTokenRepository tokens;
  private final PasswordEncoder encoder;
  private final long tokenHours;
  private final SecureRandom random = new SecureRandom();

  public AuthService(UserRepository users, AuthTokenRepository tokens, PasswordEncoder encoder,
                     @Value("${app.auth.token-hours:12}") long tokenHours) {
    this.users = users;
    this.tokens = tokens;
    this.encoder = encoder;
    this.tokenHours = tokenHours;
  }

  @Transactional
  public User register(String username, String password, String displayName) {
    var normalized = normalizeUsername(username);
    validatePassword(password);
    if (users.existsByUsername(normalized)) throw ApiException.badRequest("用户名已存在");
    var safeDisplayName = displayName == null ? normalized : displayName.trim();
    if (safeDisplayName.isBlank() || safeDisplayName.length() > 64) throw ApiException.badRequest("显示名称长度必须为 1-64 位");
    var u = new User();
    u.setUsername(normalized);
    u.setPasswordHash(encoder.encode(password));
    u.setDisplayName(safeDisplayName);
    u.setRole("USER");
    u.setEnabled(true);
    return users.save(u);
  }

  @Transactional
  public String login(String username, String password) {
    var normalized = normalizeUsernameForLogin(username);
    var u = users.findByUsername(normalized).filter(User::isEnabled).orElse(null);
    if (u == null) {
      encoder.matches(password == null ? "" : password, DUMMY_BCRYPT);
      throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }
    if (password == null || !encoder.matches(password, u.getPasswordHash()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    var raw = newToken();
    var t = new AuthToken();
    t.setTokenHash(hash(raw));
    t.setUserId(u.getId());
    t.setExpiresAt(LocalDateTime.now().plusHours(tokenHours));
    tokens.save(t);
    return raw;
  }

  public User resolve(String token) {
    if (token == null || token.isBlank() || token.length() > 256) return null;
    var at = tokens.findByTokenHashAndExpiresAtAfter(hash(token), LocalDateTime.now()).orElse(null);
    return at == null ? null : users.findById(at.getUserId()).filter(User::isEnabled).orElse(null);
  }

  public User currentUser() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
    return users.findByUsername(a.getName())
      .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在"));
  }

  @Transactional
  public void logout(String token) {
    if (token != null && !token.isBlank() && token.length() <= 256) tokens.deleteByTokenHash(hash(token));
  }

  @Scheduled(fixedDelayString = "${app.auth.cleanup-ms:3600000}")
  @Transactional
  public void purgeExpiredTokens() { tokens.deleteByExpiresAtBefore(LocalDateTime.now()); }

  public User createSeedUser(String username, String password, String name, String role) {
    var normalized = normalizeUsername(username);
    return users.findByUsername(normalized).orElseGet(() -> {
      var u = new User();
      u.setUsername(normalized);
      u.setPasswordHash(encoder.encode(password));
      u.setDisplayName(name);
      u.setRole(role);
      u.setEnabled(true);
      return users.save(u);
    });
  }

  private String normalizeUsername(String username) {
    if (username == null) throw ApiException.badRequest("用户名不能为空");
    var normalized = username.trim().toLowerCase(Locale.ROOT);
    if (!normalized.matches("[a-z0-9_]{3,32}")) throw ApiException.badRequest("用户名需为 3-32 位字母、数字或下划线");
    return normalized;
  }

  private String normalizeUsernameForLogin(String username) {
    if (username == null) return "";
    var normalized = username.trim().toLowerCase(Locale.ROOT);
    return normalized.length() > 32 ? "" : normalized;
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < 12 || password.length() > 128
        || password.chars().noneMatch(Character::isUpperCase)
        || password.chars().noneMatch(Character::isLowerCase)
        || password.chars().noneMatch(Character::isDigit))
      throw ApiException.badRequest("密码需为 12-128 位，并包含大小写字母和数字");
  }

  private String newToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String raw) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
