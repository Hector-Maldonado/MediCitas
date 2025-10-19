package com.medicitas.notification;

public interface NotificationService {
    void sendNotification(String to, String subject, String message);
}
