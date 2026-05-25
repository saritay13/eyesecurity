package com.eyesecurity.service;

import com.eyesecurity.common.AnalyticsEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalAnalyticsClientTest {
    @Test
    void sendsAnalyticsBatchSuccessfully() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalAnalyticsClient client = new ExternalAnalyticsClient(builder);

        server.expect(requestTo("https://api.heyering.com/analytics"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "eye-am-hiring"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(119611))
                .andExpect(jsonPath("$[0].asset").value("server_horizon"))
                .andExpect(jsonPath("$[0].ip").value("102.145.229.227"))
                .andExpect(jsonPath("$[0].category").value("T1190"))
                .andExpect(jsonPath("$[0].asn").value("AS3264"))
                .andExpect(jsonPath("$[0].correlationId").value(2318))
                .andRespond(withSuccess("""
                        {"status":"ok","itemsIngested":1}
                        """, MediaType.APPLICATION_JSON));

        client.submit(List.of(event()));

        server.verify();
    }

    @Test
    void throwsExceptionWhenAnalyticsReturnsTooManyRequests() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalAnalyticsClient client = new ExternalAnalyticsClient(builder);

        server.expect(requestTo("https://api.heyering.com/analytics"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.submit(List.of(event())))
                .isInstanceOf(HttpClientErrorException.TooManyRequests.class);
        server.verify();
    }

    private AnalyticsEvent event() {
        return new AnalyticsEvent(
                119611,
                "server_horizon",
                "102.145.229.227",
                "T1190",
                "AS3264",
                2318
        );
    }
}
