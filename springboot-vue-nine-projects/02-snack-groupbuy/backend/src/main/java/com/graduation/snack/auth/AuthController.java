package com.graduation.snack.auth;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
  public record LoginRequest(@NotBlank @Size(max=64) String username,@NotBlank @Size(max=128) String password){}
  public record RegisterRequest(@NotBlank @Size(max=64) String username,@NotBlank @Size(min=12,max=128) String password,@Size(max=64) String displayName){}
  @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest r) { var token=auth.login(r.username(),r.password()); var u=auth.resolve(token); return Map.of("token",token,"user",Map.of("id",u.getId(),"username",u.getUsername(),"displayName",u.getDisplayName(),"role",u.getRole())); }
  @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody RegisterRequest r) { var u=auth.register(r.username(),r.password(),r.displayName()); return Map.of("id",u.getId(),"username",u.getUsername(),"displayName",u.getDisplayName(),"role",u.getRole()); }
  @GetMapping("/me") public Map<String,Object> me() { var u=auth.currentUser(); return Map.of("id",u.getId(),"username",u.getUsername(),"displayName",u.getDisplayName(),"role",u.getRole()); }
  @PostMapping("/logout") public void logout(@RequestHeader(value="Authorization",required=false) String h) { auth.logout(h!=null&&h.startsWith("Bearer ")?h.substring(7):null); }
}
