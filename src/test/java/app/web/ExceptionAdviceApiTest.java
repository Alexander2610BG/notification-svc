package app.web;

import app.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExceptionAdvice.class)
public class ExceptionAdviceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnNotFoundErrorResponseForInvalidEndpoint() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/non-existent-endpoint"));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").value("Not supported application endpoint."));
    }
}
