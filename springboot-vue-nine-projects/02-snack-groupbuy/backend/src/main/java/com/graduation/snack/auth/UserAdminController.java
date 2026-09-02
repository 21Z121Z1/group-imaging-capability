package com.graduation.snack.auth;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/admin/users") @PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
  private final UserRepository repo; public UserAdminController(UserRepository repo){this.repo=repo;}
  public record UserView(Long id,String username,String displayName,String role,boolean enabled){}
  @GetMapping public List<UserView> list(){return repo.findAll(org.springframework.data.domain.PageRequest.of(0,200,org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"id"))).getContent().stream().map(u->new UserView(u.getId(),u.getUsername(),u.getDisplayName(),u.getRole(),u.isEnabled())).toList();}
}
