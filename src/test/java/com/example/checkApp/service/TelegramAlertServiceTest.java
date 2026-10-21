package com.example.checkApp.service;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.checkApp.entity.Check;
import com.example.checkApp.model.CheckStatus;

class TelegramAlertServiceTest {

    private static final String SEND_MESSAGE_URL = "https://api.telegram.org/bottok123/sendMessage";

    private Check check() {
        Check check = new Check();
        check.setName("google");
        check.setUrl("https://google.com");
        check.setLastStatus(CheckStatus.UP);
        return check;
    }

    private TelegramAlertService service(RestClient.Builder builder, String token, String chatId) {
        return new TelegramAlertService(builder, token, chatId);
    }

    @Test
    void logsInsteadOfSendingWhenNotConfigured() {
        TelegramAlertService service = service(RestClient.builder(), "", "");

        service.notifyStatusChange(check(), CheckStatus.UP, CheckStatus.DOWN);
        service.notifyStatusChange(check(), CheckStatus.DOWN, CheckStatus.UP);
    }

    @Test
    void sendsPostToTelegramApiWhenConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SEND_MESSAGE_URL))
                .andExpect(method(POST))
                .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON));

        service(builder, "tok123", "chat-1").notifyStatusChange(check(), CheckStatus.UP, CheckStatus.DOWN);

        server.verify();
    }

    @Test
    void swallowsRestErrorsWithoutThrowing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SEND_MESSAGE_URL))
                .andRespond(withServerError());

        service(builder, "tok123", "chat-1").notifyStatusChange(check(), CheckStatus.UP, CheckStatus.DOWN);

        server.verify();
    }
}
