package com.graduation.lab;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CoreWorkflowTests {
  @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
  private String token() throws Exception {
    var r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"student\",\"password\":\"Student123!Demo\"}")).andExpect(status().isOk()).andReturn();
    return mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
  }
  @Test void coreWorkflowAndInvariant() throws Exception {
    var h="Bearer "+token();
    mvc.perform(post("/api/reservations").header("Authorization",h).contentType(MediaType.APPLICATION_JSON).content("{\"labId\":1,\"title\":\"A\",\"purpose\":\"test\",\"startTime\":\"2099-01-02T10:00:00\",\"endTime\":\"2099-01-02T12:00:00\"}")).andExpect(status().isOk());
    mvc.perform(post("/api/reservations").header("Authorization",h).contentType(MediaType.APPLICATION_JSON).content("{\"labId\":1,\"title\":\"B\",\"purpose\":\"overlap\",\"startTime\":\"2099-01-02T11:00:00\",\"endTime\":\"2099-01-02T13:00:00\"}")).andExpect(status().is(400));
  }
}
