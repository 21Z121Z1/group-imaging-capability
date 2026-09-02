package com.graduation.rental;
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
    mvc.perform(post("/api/appointments").header("Authorization",h).contentType(MediaType.APPLICATION_JSON).content("{\"listingId\":1,\"visitTime\":\"2099-01-01T10:00:00\",\"note\":\"test\"}")).andExpect(status().isOk());
    mvc.perform(post("/api/appointments").header("Authorization",h).contentType(MediaType.APPLICATION_JSON).content("{\"listingId\":1,\"visitTime\":\"2020-01-01T10:00:00\",\"note\":\"past\"}")).andExpect(status().is(400));
  }
}
