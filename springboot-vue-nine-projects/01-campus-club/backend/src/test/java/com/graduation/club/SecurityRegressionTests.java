package com.graduation.club;

import com.graduation.club.auth.AuthTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRegressionTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired AuthTokenRepository tokens;

  private String login(String username, String password) throws Exception {
    var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
      .content(json.writeValueAsString(Map.of("username",username,"password",password))))
      .andExpect(status().isOk()).andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  @Test void adminApiRequiresAuthenticationAndRole() throws Exception {
    mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    String student = login("student", "Student123!Demo");
    mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+student)).andExpect(status().isForbidden());
  }

  @Test void invalidRegistrationAndMalformedJsonAreClientErrors() throws Exception {
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
      .content("{\"username\":\"weakuser\",\"password\":\"weak\"}"))
      .andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
      .andExpect(status().isBadRequest());
  }

  @Test void bearerSecretsAreHashedAndUserViewsDoNotLeakPasswordHashes() throws Exception {
    String admin = login("admin", "Admin123!Demo");
    assertThat(tokens.findAll()).allSatisfy(t -> {
      assertThat(t.getTokenHash()).hasSize(64);
      assertThat(t.getTokenHash()).isNotEqualTo(admin);
    });
    mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+admin))
      .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))));
  }

  @Test void requestCorrelationIdIsSafeAndReturned() throws Exception {
    mvc.perform(get("/actuator/health").header("X-Request-Id","safe-id-123"))
      .andExpect(status().isOk()).andExpect(header().string("X-Request-Id","safe-id-123"));
    var response = mvc.perform(get("/actuator/health").header("X-Request-Id","bad id/value"))
      .andExpect(status().isOk()).andReturn().getResponse().getHeader("X-Request-Id");
    assertThat(response).isNotBlank().doesNotContain(" ","/");
  }
}
