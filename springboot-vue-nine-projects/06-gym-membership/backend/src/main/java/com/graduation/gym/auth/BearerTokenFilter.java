package com.graduation.gym.auth;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.util.List;
@Component
public class BearerTokenFilter extends OncePerRequestFilter {
  private final AuthService auth; public BearerTokenFilter(AuthService auth){this.auth=auth;}
  @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
    var h=req.getHeader("Authorization");
    if(h!=null&&h.startsWith("Bearer ")&&SecurityContextHolder.getContext().getAuthentication()==null) {
      var u=auth.resolve(h.substring(7)); if(u!=null) { var a=new UsernamePasswordAuthenticationToken(u.getUsername(),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))); SecurityContextHolder.getContext().setAuthentication(a); }
    } chain.doFilter(req,res);
  }
}
