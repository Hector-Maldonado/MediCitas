package com.medicitas.service.factory;

import com.medicitas.notification.EmailNotificationService;
import com.medicitas.notification.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    private final EmailNotificationService emailNotificationService;

    public NotificationFactory(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    public NotificationService createNotification(String type) {
        if ("email".equalsIgnoreCase(type)) {
            return emailNotificationService;
        }
        throw new IllegalArgumentException("Tipo de notificación no soportado: " + type);
    }
}