package com.lagosrent.notification.infrastructure.provider.mock;

import com.lagosrent.notification.infrastructure.provider.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "notification.provider.active", havingValue = "mock", matchIfMissing = true)
public class MockEmailProvider implements EmailProvider {
    @Override
    public String getProviderName() {
        return "MOCK_EMAIL_PROVIDER";
    }

    @Override
    public EmailDispatchResult sendEmail(EmailDispatchCommand command) {
        String mockMessageId = "mock-msg-" + randomUUID().toString().substring(0, 8);

        log.info("""
                ==================== [MOCK EMAIL DISPATCH] ====================
                To:      {}
                Subject: {}
                HTML:    {}
                Msg ID:  {}
                Body:
                {}
                ===============================================================
                """, command.to(), command.subject(), command.isHtml(), mockMessageId, command.body());

        return new EmailDispatchResult(true, mockMessageId, null);
    }
}
