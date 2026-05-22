package com.kosmoskan.apigateway;

import com.kosmoskan.apigateway.controller.GatewayController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(GatewayController.class)
@TestPropertySource(properties = {
    "app.file-storing-url=http://localhost:19999",
    "app.file-analysis-url=http://localhost:19998"
})
class GatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_GatewayAlwaysReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.gateway").value("ok"));
    }

    @Test
    void health_UnavailableServices_ShownCorrectly() throws Exception {
        mockMvc.perform(get("/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.services.file_storing").value("unavailable"))
               .andExpect(jsonPath("$.services.file_analysis").value("unavailable"));
    }

    @Test
    void listWorks_StoringUnavailable_Returns503() throws Exception {
        mockMvc.perform(get("/works"))
               .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getWork_StoringUnavailable_Returns503() throws Exception {
        mockMvc.perform(get("/works/1"))
               .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getWorkReport_AnalysisUnavailable_Returns503() throws Exception {
        mockMvc.perform(get("/works/1/reports"))
               .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getWordcloud_AnalysisUnavailable_Returns503() throws Exception {
        mockMvc.perform(get("/works/1/wordcloud"))
               .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getAllReports_AnalysisUnavailable_Returns503() throws Exception {
        mockMvc.perform(get("/reports"))
               .andExpect(status().isServiceUnavailable());
    }
}
