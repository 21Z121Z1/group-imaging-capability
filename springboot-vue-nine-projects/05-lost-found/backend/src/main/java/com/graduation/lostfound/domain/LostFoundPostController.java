package com.graduation.lostfound.domain;
import com.graduation.lostfound.auth.AuthService;
import com.graduation.lostfound.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class LostFoundPostController {
  private final LostFoundPostRepository repo; private final AuthService auth;
  public LostFoundPostController(LostFoundPostRepository repo, AuthService auth) { this.repo=repo; this.auth=auth; }
  public record Create(@jakarta.validation.constraints.NotBlank String type,
                       @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String title,
                       @jakarta.validation.constraints.Size(max=128) String category,
                       @jakarta.validation.constraints.Size(max=255) String location,
                       LocalDateTime eventTime,
                       @jakarta.validation.constraints.Size(max=3000) String description,
                       @jakarta.validation.constraints.Size(max=255) String contact,
                       @jakarta.validation.constraints.Size(max=512) String imageUrl) {}
  @GetMapping public List<LostFoundPost> list() { return repo.findAll(PageRequest.of(0,200,Sort.by(Sort.Direction.DESC,"id"))).getContent(); }
  @PostMapping public LostFoundPost create(@Valid @RequestBody Create v) {
    if(!List.of("LOST","FOUND").contains(v.type())) throw ApiException.badRequest("类型必须为 LOST 或 FOUND");
    var x=new LostFoundPost(); x.setType(v.type()); x.setTitle(v.title()); x.setCategory(v.category()); x.setLocation(v.location()); x.setEventTime(v.eventTime());
    x.setDescription(v.description()); x.setContact(v.contact()); x.setImageUrl(v.imageUrl()); x.setOwnerUserId(auth.currentUser().getId()); x.setStatus("OPEN"); return repo.save(x);
  }
}
