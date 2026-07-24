package com.clara.insurancequotes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.clara.insurancequotes.testsupport.Containers;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:1"})
@AutoConfigureMockMvc
class OpenApiExportIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        Containers.registerPostgres(registry);
        Containers.registerRedis(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exportsCommittedContract() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs.yaml")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        var yaml = result.getResponse().getContentAsString();

        assertThat(yaml).contains("/quotes/{id}/coverage").contains("/auth/login");

        var target = Path.of("..", "docs", "api", "openapi.yaml");
        Files.createDirectories(target.getParent());
        Files.writeString(target, yaml);
        assertThat(Files.size(target)).isGreaterThan(1000);
    }
}
