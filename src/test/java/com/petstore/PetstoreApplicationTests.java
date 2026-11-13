package com.petstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Smoke tests: the application starts and reports itself healthy. */
@SpringBootTest
@AutoConfigureMockMvc
class PetstoreApplicationTests {

  @Autowired private MockMvcTester mvc;

  @Test
  void contextLoads() {}

  @Test
  void healthEndpointReportsUp() {
    assertThat(mvc.get().uri("/actuator/health"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("UP");
  }
}
