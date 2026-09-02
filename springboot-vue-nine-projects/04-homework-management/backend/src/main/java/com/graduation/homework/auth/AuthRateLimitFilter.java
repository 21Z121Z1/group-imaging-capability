package com.graduation.homework.auth;
import jakarta.servlet.*;import jakarta.servlet.http.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;import java.io.IOException;import java.time.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicInteger;
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
  private record Window(long minute,AtomicInteger count){}
  private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>(); private final int limit;
  public AuthRateLimitFilter(@Value("${app.security.auth-rate-limit-per-minute:12}") int limit){this.limit=limit;}
  @Override protected boolean shouldNotFilter(HttpServletRequest r){var p=r.getRequestURI();return !("POST".equals(r.getMethod())&&(p.equals("/api/auth/login")||p.equals("/api/auth/register")));}
  @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
    long minute=Instant.now().getEpochSecond()/60;String key=req.getRemoteAddr()+"|"+req.getRequestURI();
    if(windows.size()>10000&&!windows.containsKey(key)){res.setStatus(429);res.setHeader("Retry-After","60");return;}
    var w=windows.compute(key,(k,v)->v==null||v.minute()!=minute?new Window(minute,new AtomicInteger(1)):(v.count().incrementAndGet()>0?v:v));
    if(w.count().get()>limit){res.setStatus(429);res.setContentType("application/json");res.setHeader("Retry-After","60");res.getWriter().write("{\"status\":429,\"message\":\"请求过于频繁\"}");return;}
    if(windows.size()>5000)windows.entrySet().removeIf(e->e.getValue().minute()<minute-1); chain.doFilter(req,res);
  }
}
