package com.psyavocat.service.impl;

import com.psyavocat.dto.notification.NotificationResponseDTO;
import com.psyavocat.entity.Notification;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.NotificationRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext
    ) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMesNotifications() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return notificationRepository.findByDestinataireIdOrderByDateEnvoiDesc(uid).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void sendNotification(String destinataireId, String type, String contenu, String lienVisio) {
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElse(null);
        if (destinataire != null) {
            Notification notification = new Notification();
            notification.setType(type);
            notification.setContenu(contenu);
            notification.setDateEnvoi(LocalDateTime.now());
            notification.setLienVisio(lienVisio);
            notification.setDestinataire(destinataire);
            notificationRepository.save(notification);
        }
    }

    @Override
    public void deleteNotification(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));

        if (!notif.getDestinataire().getId().equals(uid)) {
            throw new ForbiddenException("Vous ne pouvez pas supprimer cette notification");
        }
        
        notificationRepository.delete(notif);
    }

    private NotificationResponseDTO toDto(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .contenu(n.getContenu())
                .dateEnvoi(n.getDateEnvoi())
                .lienVisio(n.getLienVisio())
                .build();
    }
}
