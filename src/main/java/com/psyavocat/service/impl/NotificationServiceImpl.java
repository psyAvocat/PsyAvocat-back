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
import com.psyavocat.service.notification.NotificationDispatcher;
import com.psyavocat.service.notification.NotificationEvenement;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext,
            ApplicationEventPublisher eventPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
        this.eventPublisher = eventPublisher;
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
    public void notifier(NotificationEvenement evenement) {
        Utilisateur destinataire = utilisateurRepository.findById(evenement.destinataireId()).orElse(null);
        if (destinataire == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setType(evenement.type());
        notification.setTitre(evenement.titre() != null ? evenement.titre() : titreParDefaut(evenement.type()));
        notification.setContenu(evenement.message());
        notification.setDateEnvoi(LocalDateTime.now());
        notification.setLienVisio(evenement.lienVisio());
        notification.setUnivers(evenement.univers());
        notification.setRessourceType(evenement.ressourceType());
        notification.setRessourceId(evenement.ressourceId());
        notification.setDestinataire(destinataire);

        Notification saved = notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationDispatcher.NotificationCreee(destinataire.getId(), toDto(saved)));
    }

    @Override
    public void sendNotification(String destinataireId, String type, String contenu, String lienVisio) {
        notifier(new NotificationEvenement(destinataireId, type, null, contenu, null, null, null, lienVisio));
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return notificationRepository.countByDestinataireIdAndLuFalse(uid);
    }

    @Override
    public void markAsRead(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));

        if (!notif.getDestinataire().getId().equals(uid)) {
            throw new ForbiddenException("Vous ne pouvez pas modifier cette notification");
        }
        if (!notif.isLu()) {
            notif.setLu(true);
            notif.setDateLecture(LocalDateTime.now());
            notificationRepository.save(notif);
        }
    }

    @Override
    public void markAllAsRead() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        LocalDateTime maintenant = LocalDateTime.now();
        List<Notification> list = notificationRepository.findByDestinataireIdOrderByDateEnvoiDesc(uid);
        for (Notification n : list) {
            if (!n.isLu()) {
                n.setLu(true);
                n.setDateLecture(maintenant);
            }
        }
        notificationRepository.saveAll(list);
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

    /** Titre lisible pour les notifications créées sans titre explicite (appelants historiques). */
    private String titreParDefaut(String type) {
        if (type == null) {
            return "PsyAvocat";
        }
        if (type.startsWith("RAPPEL_RDV")) {
            return "Rappel de rendez-vous";
        }
        return switch (type) {
            case "INSCRIPTION_PRO" -> "Nouvelle demande d'inscription";
            case "NOUVEAU_MESSAGE" -> "Nouveau message";
            default -> "PsyAvocat";
        };
    }

    private NotificationResponseDTO toDto(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .contenu(n.getContenu())
                .dateEnvoi(n.getDateEnvoi())
                .lienVisio(n.getLienVisio())
                .lu(n.isLu())
                .titre(n.getTitre())
                .dateLecture(n.getDateLecture())
                .univers(n.getUnivers())
                .ressourceType(n.getRessourceType())
                .ressourceId(n.getRessourceId())
                .build();
    }
}
