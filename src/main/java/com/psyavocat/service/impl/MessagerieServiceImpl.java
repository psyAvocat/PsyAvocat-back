package com.psyavocat.service.impl;

import com.psyavocat.dto.messagerie.*;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Conversation;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.MessageContact;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.ConversationRepository;
import com.psyavocat.repository.FichePatientRepository;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.repository.SoumissionDossierRepository;
import com.psyavocat.service.support.AccesProfessionnel;
import com.psyavocat.repository.MessageContactRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.MessagerieService;
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.notification.NotificationEvenement;
import com.psyavocat.service.support.ClientAccounts;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.mapper.MediaUrlResolver;
import com.psyavocat.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.psyavocat.service.support.Entites;

@Service
@Transactional
public class MessagerieServiceImpl implements MessagerieService {

    private final ConversationRepository conversationRepository;
    private final MessageContactRepository messageContactRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;
    private final NotificationService notificationService;
    private final RealtimeGateway realtimeGateway;
    private final MediaUrlResolver mediaUrlResolver;
    private final AccesProfessionnel accesProfessionnel;
    private final RendezVousRepository rendezVousRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final FichePatientRepository fichePatientRepository;

    public MessagerieServiceImpl(
            ConversationRepository conversationRepository,
            MessageContactRepository messageContactRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext,
            NotificationService notificationService,
            RealtimeGateway realtimeGateway,
            MediaUrlResolver mediaUrlResolver,
            AccesProfessionnel accesProfessionnel,
            RendezVousRepository rendezVousRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            FichePatientRepository fichePatientRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageContactRepository = messageContactRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
        this.notificationService = notificationService;
        this.realtimeGateway = realtimeGateway;
        this.mediaUrlResolver = mediaUrlResolver;
        this.accesProfessionnel = accesProfessionnel;
        this.rendezVousRepository = rendezVousRepository;
        this.soumissionDossierRepository = soumissionDossierRepository;
        this.fichePatientRepository = fichePatientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getMesConversations() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return conversationRepository.findByParticipantId(uid).stream()
                .map(c -> toConversationDto(c, uid))
                .toList();
    }

    @Override
    public List<MessageResponseDTO> getMessages(String conversationId) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));

        boolean isParticipant = conv.getParticipants().stream().anyMatch(p -> p.getId().equals(uid));
        if (!isParticipant) {
            throw new ForbiddenException("Vous ne faites pas partie de cette conversation");
        }

        List<MessageContact> messages = messageContactRepository.findByConversationIdOrderByDateEnvoiAsc(conversationId);
        boolean updated = false;
        for (MessageContact m : messages) {
            if (m.getExpediteur() != null && !m.getExpediteur().getId().equals(uid) && (m.getLu() == null || !m.getLu())) {
                m.setLu(true);
                updated = true;
            }
        }
        if (updated) {
            messageContactRepository.saveAll(messages);
        }
        // Conversation lue : ses notifications « Nouveau message » le sont aussi (cloche à jour).
        notificationService.marquerLuesPourRessource(uid, "CONVERSATION", conversationId);

        return messages.stream()
                .map(this::toMessageDto)
                .toList();
    }

    @Override
    public ConversationResponseDTO createConversation(CreateConversationRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur expediteur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Utilisateur destinataire = utilisateurRepository.findById(request.getDestinataireId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire introuvable"));

        // Une seule conversation par paire de personnes : on la réutilise si elle existe
        // (les droits y sont vérifiés par sendMessage, réponse à une prise de contact comprise).
        Conversation existante = conversationRepository.findByParticipantId(uid).stream()
                .filter(c -> c.getParticipants().stream().anyMatch(p -> p.getId().equals(destinataire.getId())))
                .findFirst()
                .orElse(null);
        if (existante != null) {
            sendMessage(existante.getId(), new SendMessageRequest(request.getObjet(), request.getPremierMessage()));
            return toConversationDto(conversationRepository.findById(existante.getId()).orElse(existante), uid);
        }

        verifierDestinataireAutorise(expediteur, destinataire, false);

        Conversation conv = new Conversation();
        conv.setDateCreation(LocalDateTime.now());
        conv.setStatut("OUVERTE");
        conv.setParticipants(new ArrayList<>(List.of(expediteur, destinataire)));
        conv.setMessages(new ArrayList<>());

        Conversation savedConv = conversationRepository.save(conv);

        MessageContact message = new MessageContact();
        message.setConversation(savedConv);
        message.setExpediteur(expediteur);
        message.setDateEnvoi(LocalDateTime.now());
        message.setObjet(request.getObjet());
        message.setContenu(request.getPremierMessage());

        MessageContact savedMsg = messageContactRepository.save(message);
        savedConv.getMessages().add(savedMsg);

        diffuserNouveauMessage(savedConv, expediteur, savedMsg);
        return toConversationDto(savedConv, uid);
    }

    @Override
    public MessageResponseDTO sendMessage(String conversationId, SendMessageRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));

        boolean isParticipant = conv.getParticipants().stream().anyMatch(p -> p.getId().equals(uid));
        if (!isParticipant) {
            throw new ForbiddenException("Vous ne faites pas partie de cette conversation");
        }

        Utilisateur expediteur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        // Les droits sont revérifiés à chaque envoi (statut ou relation ayant pu changer).
        for (Utilisateur participant : conv.getParticipants()) {
            if (!participant.getId().equals(uid)) {
                boolean aDejaEcrit = messageContactRepository
                        .existsByConversationIdAndExpediteurId(conv.getId(), participant.getId());
                verifierDestinataireAutorise(expediteur, participant, aDejaEcrit);
            }
        }

        MessageContact message = new MessageContact();
        message.setConversation(conv);
        message.setExpediteur(expediteur);
        message.setDateEnvoi(LocalDateTime.now());
        message.setObjet(request.getObjet());
        message.setContenu(request.getContenu());

        MessageContact saved = messageContactRepository.save(message);
        diffuserNouveauMessage(conv, expediteur, saved);
        return toMessageDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getNombreMessagesNonLus() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return messageContactRepository.countUnreadMessagesForUser(uid);
    }

    /**
     * Règles d'échange :
     * - un client écrit uniquement à un professionnel approuvé et actif ;
     * - un professionnel approuvé écrit à SES clients (rendez-vous, dossier reçu ou fiche patient)
     *   et répond à toute personne qui l'a contacté (même sans relation existante) ;
     * - toute autre combinaison (administrateur, professionnel à professionnel, soi-même) est refusée.
     *
     * @param destinataireADejaEcrit le destinataire a déjà écrit dans la conversation
     */
    private void verifierDestinataireAutorise(Utilisateur expediteur, Utilisateur destinataire,
                                              boolean destinataireADejaEcrit) {
        expediteur = Entites.reel(expediteur);
        destinataire = Entites.reel(destinataire);
        if (expediteur.getId().equals(destinataire.getId())) {
            throw new BadRequestException("Vous ne pouvez pas vous envoyer de message");
        }
        if (Boolean.FALSE.equals(destinataire.getActif())) {
            throw new BadRequestException("Ce destinataire n'est plus disponible");
        }
        if (ClientAccounts.isClient(expediteur)) {
            if (!(destinataire instanceof Professionnel pro) || !AccesProfessionnel.estApprouve(pro)) {
                throw new ForbiddenException("Vous ne pouvez contacter qu'un professionnel validé");
            }
            return;
        }
        if (expediteur instanceof Professionnel pro) {
            accesProfessionnel.verifierApprouve(pro);
            if (!ClientAccounts.isClient(destinataire)) {
                throw new ForbiddenException("La messagerie est réservée aux échanges entre clients et professionnels");
            }
            if (!destinataireADejaEcrit && !estClientDe(pro, destinataire)) {
                throw new ForbiddenException(
                        "Vous ne pouvez écrire qu'à vos clients ou répondre à une personne qui vous a contacté");
            }
            return;
        }
        throw new ForbiddenException("La messagerie est réservée aux échanges entre clients et professionnels");
    }

    /** Relation réelle entre le professionnel et le client. */
    private boolean estClientDe(Professionnel pro, Utilisateur client) {
        if (rendezVousRepository.existsByProfessionnelIdAndPatientId(pro.getId(), client.getId())) {
            return true;
        }
        if (pro instanceof Avocat) {
            return soumissionDossierRepository.existsByAvocatIdAndDossierJusticiableId(pro.getId(), client.getId());
        }
        if (pro instanceof Psychologue) {
            return fichePatientRepository.findByPsychologueIdAndPatientId(pro.getId(), client.getId()).isPresent();
        }
        return false;
    }

    /**
     * Nouveau message : notification persistée (+ push) pour les autres participants,
     * et événement temps réel pour tous (conversation ouverte sur un autre appareil incluse).
     */
    private void diffuserNouveauMessage(Conversation conv, Utilisateur expediteur, MessageContact message) {
        MessageResponseDTO dto = toMessageDto(message);
        for (Utilisateur participant : conv.getParticipants()) {
            realtimeGateway.envoyerA(participant.getId(), "MESSAGE", dto);
            if (!participant.getId().equals(expediteur.getId())) {
                notificationService.notifier(new NotificationEvenement(
                        participant.getId(),
                        "NOUVEAU_MESSAGE",
                        "Nouveau message de " + expediteur.getPrenom() + " " + expediteur.getNom(),
                        apercu(message.getContenu()),
                        universDeConversation(conv),
                        "CONVERSATION",
                        conv.getId(),
                        null));
            }
        }
    }

    /** Univers d'une conversation : celui du professionnel qui y participe. */
    private String universDeConversation(Conversation conv) {
        return conv.getParticipants().stream()
                .map(this::typeDe)
                .filter(t -> "AVOCAT".equals(t) || "PSYCHOLOGUE".equals(t))
                .findFirst()
                .orElse(null);
    }

    private String typeDe(Utilisateur u) {
        u = Entites.reel(u);
        if (u instanceof Avocat) {
            return "AVOCAT";
        }
        if (u instanceof Psychologue) {
            return "PSYCHOLOGUE";
        }
        return u != null && ClientAccounts.isClient(u) ? "CLIENT" : null;
    }

    private String apercu(String contenu) {
        if (contenu == null) {
            return "";
        }
        return contenu.length() > 120 ? contenu.substring(0, 117) + "..." : contenu;
    }

    private ConversationResponseDTO toConversationDto(Conversation c, String currentUserId) {
        Utilisateur correspondant = c.getParticipants().stream()
                .filter(p -> !p.getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        MessageResponseDTO dernierMsg = null;
        if (c.getMessages() != null && !c.getMessages().isEmpty()) {
            // La collection n'est pas triée (ordre des identifiants) : le dernier
            // message est le plus récent par date d'envoi.
            MessageContact last = c.getMessages().stream()
                    .max(java.util.Comparator.comparing(MessageContact::getDateEnvoi,
                            java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                    .orElseThrow();
            dernierMsg = toMessageDto(last);
        }

        long nonLus = (c.getMessages() != null)
                ? c.getMessages().stream()
                    .filter(m -> m.getExpediteur() != null && !m.getExpediteur().getId().equals(currentUserId) && (m.getLu() == null || !m.getLu()))
                    .count()
                : 0L;

        List<String> participantIds = c.getParticipants().stream().map(Utilisateur::getId).toList();

        return ConversationResponseDTO.builder()
                .id(c.getId())
                .dateCreation(c.getDateCreation())
                .statut(c.getStatut())
                .participantIds(participantIds)
                .correspondantId(correspondant != null ? correspondant.getId() : null)
                .correspondantNom(correspondant != null ? correspondant.getNom() : null)
                .correspondantPrenom(correspondant != null ? correspondant.getPrenom() : null)
                .correspondantType(typeDe(correspondant))
                .correspondantPhotoUrl(correspondant != null ? mediaUrlResolver.photoUrlOf(correspondant) : null)
                .dernierMessage(dernierMsg)
                .messagesNonLus(nonLus)
                .build();
    }

    private MessageResponseDTO toMessageDto(MessageContact m) {
        MessageResponseDTO.MessageResponseDTOBuilder builder = MessageResponseDTO.builder()
                .id(m.getId())
                .conversationId(m.getConversation() != null ? m.getConversation().getId() : null)
                .objet(m.getObjet())
                .contenu(m.getContenu())
                .dateEnvoi(m.getDateEnvoi())
                .lu(m.getLu() != null ? m.getLu() : false);

        if (m.getExpediteur() != null) {
            builder.expediteurId(m.getExpediteur().getId())
                    .expediteurNom(m.getExpediteur().getNom())
                    .expediteurPrenom(m.getExpediteur().getPrenom());
        }

        return builder.build();
    }
}
