package com.eyesecurity.service;

import com.eyesecurity.common.EnrichmentResponse;
import com.eyesecurity.common.SecurityCategory;
import com.eyesecurity.common.SecurityLogRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalEnrichmentClientTest {
    @Test
    void sendsEnrichmentRequestAndParsesSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalEnrichmentClient client = new ExternalEnrichmentClient(builder);

        server.expect(requestTo("https://api.heyering.com/enrichment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "eye-am-hiring"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(119611))
                .andExpect(jsonPath("$.asset").value("server_horizon"))
                .andExpect(jsonPath("$.ip").value("102.145.229.227"))
                .andExpect(jsonPath("$.category").value("phishing"))
                .andRespond(withSuccess("""
                        {"asn":"AS3264","category":"T1190","correlationId":2318}
                        """, MediaType.APPLICATION_JSON));

        EnrichmentResponse response = client.enrich(record());

        assertThat(response.asn()).isEqualTo("AS3264");
        assertThat(response.category()).isEqualTo("T1190");
        assertThat(response.correlationId()).isEqualTo(2318);
        server.verify();
    }

    @Test
    void throwsExceptionWhenEnrichmentReturnsClientError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalEnrichmentClient client = new ExternalEnrichmentClient(builder);

        server.expect(requestTo("https://api.heyering.com/enrichment"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.enrich(record()))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
        server.verify();
    }

    private SecurityLogRecord record() {
        return new SecurityLogRecord(
                119611,
                "server_horizon",
                "102.145.229.227",
                LocalDateTime.parse("2024-02-27T00:00:00"),
                "pxtrpf",
                SecurityCategory.PHISHING
        );
    }
}
