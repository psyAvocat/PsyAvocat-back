package com.psyavocat.service;

import com.psyavocat.dto.notification.NotificationResponseDTO;
import java.util.List;

public interface NotificationService {
    List<NotificationResponseDTO> getMesNotifications();
    void sendNotification(String destinataireId, String type, String contenu, String lienVisio);
    void deleteNotification(String id);
}
