package com.lagosrent.notification.infrastructure.provider;

public interface EmailProvider {
    String getProviderName();

    EmailDispatchResult sendEmail(EmailDispatchCommand command);

    record EmailDispatchCommand(
            String to,
            String subject,
            String body,
            boolean isHtml
    ) {}

    record EmailDispatchResult(
            boolean success,
            String providerMessageId,
            String errorMessage
    ) {}
}
