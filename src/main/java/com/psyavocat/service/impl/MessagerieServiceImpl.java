package com.psyavocat.service.impl;

import com.psyavocat.dto.messagerie.*;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Conversation;
import com.psyavocat.entity.ConversationMasquee;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.MessageContact;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.ConversationMasqueeRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.psyavocat.service.support.ApresValidation;
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
    private final ConversationMasqueeRepository conversationMasqueeRepository;

    /** Ressource des notifications « Nouveau message » (lien profond vers la conversation). */
    private static final String RESSOURCE_CONVERSATION = "CONVERSATION";

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
            FichePatientRepository fichePatientRepository,
            ConversationMasqueeRepository conversationMasqueeRepository
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
        this.conversationMasqueeRepository = conversationMasqueeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getMesConversations() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Map<String, LocalDateTime> masquages = new HashMap<>();
        conversationMasqueeRepository.findByUtilisateurId(uid)
                .forEach(m -> masquages.put(m.getConversation().getId(), m.getDateMasquage()));
        return conversationRepository.findByParticipantId(uid).stream()
                // Discussion supprimée par l'appelant : elle ne réapparaît qu'avec un nouveau message.
                .filter(c -> !masquages.containsKey(c.getId())
                        || messagesVisibles(c, masquages.get(c.getId())).findAny().isPresent())
                .map(c -> toConversationDto(c, uid, masquages.get(c.getId())))
                .toList();
    }

    /** Messages postérieurs à la suppression de la discussion par l'appelant (tous si jamais supprimée). */
    private java.util.stream.Stream<MessageContact> messagesVisibles(Conversation c, LocalDateTime depuis) {
        java.util.stream.Stream<MessageContact> messages = c.getMessages() != null ? c.getMessages().stream()
                : java.util.stream.Stream.empty();
        return depuis == null ? messages
                : messages.filter(m -> m.getDateEnvoi() != null && m.getDateEnvoi().isAfter(depuis));
    }

    private LocalDateTime masqueeLe(String conversationId, String uid) {
        return conversationMasqueeRepository.findByConversationIdAndUtilisateurId(conversationId, uid)
                .map(ConversationMasquee::getDateMasquage)
                .orElse(null);
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
        notificationService.marquerLuesPourRessource(uid, RESSOURCE_CONVERSATION, conversationId);

        LocalDateTime depuis = masqueeLe(conversationId, uid);
        return messages.stream()
                .filter(m -> depuis == null || (m.getDateEnvoi() != null && m.getDateEnvoi().isAfter(depuis)))
                .map(this::toMessageDto)
                .toList();
    }

    /** Une seule conversation par paire de personnes. */
    private Conversation conversationEntre(String uid, String autreId) {
        return conversationRepository.findByParticipantId(uid).stream()
                .filter(c -> c.getParticipants().stream().anyMatch(p -> p.getId().equals(autreId)))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponseDTO ouvrirAvec(String destinataireId) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur expediteur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire introuvable"));

        Conversation existante = conversationEntre(uid, destinataireId);
        LocalDateTime depuis = existante != null ? masqueeLe(existante.getId(), uid) : null;
        if (existante != null && messagesVisibles(existante, depuis).findAny().isPresent()) {
            return toConversationDto(existante, uid, depuis);
        }

        // Aucune discussion visible : on vérifie dès maintenant que l'appelant peut écrire (même règle
        // qu'au premier envoi), puis on renvoie un brouillon. Abandonné, il ne laisse aucune trace.
        boolean aDejaEcrit = existante != null && messageContactRepository
                .existsByConversationIdAndExpediteurId(existante.getId(), destinataireId);
        verifierDestinataireAutorise(expediteur, destinataire, aDejaEcrit);

        Conversation brouillon = new Conversation();
        brouillon.setParticipants(List.of(expediteur, destinataire));
        brouillon.setMessages(List.of());
        return toConversationDto(brouillon, uid, null);
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
        Conversation existante = conversationEntre(uid, destinataire.getId());
        if (existante != null) {
            sendMessage(existante.getId(), new SendMessageRequest(request.getObjet(), request.getPremierMessage()));
            return toConversationDto(conversationRepository.findById(existante.getId()).orElse(existante), uid,
                    masqueeLe(existante.getId(), uid));
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
        return toConversationDto(savedConv, uid, null);
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
    public void supprimerMessage(String conversationId, String messageId) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Conversation conv = conversationDuParticipant(conversationId, uid);
        MessageContact message = messageContactRepository.findById(messageId)
                .filter(m -> m.getConversation() != null && conversationId.equals(m.getConversation().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Message introuvable"));
        if (message.getExpediteur() == null || !uid.equals(message.getExpediteur().getId())) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres messages");
        }
        boolean nonLu = message.getLu() == null || !message.getLu();

        conv.getMessages().removeIf(m -> messageId.equals(m.getId()));
        messageContactRepository.delete(message);
        // Plus aucun message : une discussion vide n'est pas conservée.
        boolean vide = conv.getMessages().isEmpty();

        Map<String, Object> evenement = new HashMap<>();
        evenement.put("conversationId", conversationId);
        evenement.put("id", messageId);
        evenement.put("supprime", true);
        if (vide) {
            evenement.put("conversationSupprimee", true);
        }
        for (Utilisateur participant : conv.getParticipants()) {
            // Message non encore lu : sa notification (avec aperçu) disparaît de la cloche du destinataire.
            if (nonLu && !participant.getId().equals(uid)) {
                notificationService.supprimerNonLuesPourRessource(participant.getId(), RESSOURCE_CONVERSATION, conversationId);
            }
            String destinataire = participant.getId();
            ApresValidation.executer(() -> realtimeGateway.envoyerA(destinataire, "MESSAGE", evenement));
        }
        if (vide) {
            effacer(conv);
        }
    }

    /** Efface la discussion de la base, avec ses suppressions individuelles et ses notifications. */
    private void effacer(Conversation conv) {
        conversationMasqueeRepository.deleteAll(conversationMasqueeRepository.findByConversationId(conv.getId()));
        notificationService.supprimerPourRessource(RESSOURCE_CONVERSATION, conv.getId());
        conversationRepository.delete(conv);
    }

    @Override
    public void supprimerConversation(String conversationId) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Conversation conv = conversationDuParticipant(conversationId, uid);
        LocalDateTime maintenant = LocalDateTime.now();

        // Les messages reçus sont considérés comme lus : plus de badge pour une discussion supprimée.
        List<MessageContact> recus = conv.getMessages().stream()
                .filter(m -> m.getExpediteur() != null && !uid.equals(m.getExpediteur().getId())
                        && (m.getLu() == null || !m.getLu()))
                .toList();
        recus.forEach(m -> m.setLu(true));
        messageContactRepository.saveAll(recus);
        notificationService.marquerLuesPourRessource(uid, RESSOURCE_CONVERSATION, conversationId);

        ConversationMasquee masquage = conversationMasqueeRepository
                .findByConversationIdAndUtilisateurId(conversationId, uid)
                .orElseGet(() -> {
                    ConversationMasquee m = new ConversationMasquee();
                    m.setConversation(conv);
                    m.setUtilisateur(conv.getParticipants().stream()
                            .filter(p -> p.getId().equals(uid)).findFirst().orElseThrow());
                    return m;
                });
        masquage.setDateMasquage(maintenant);
        conversationMasqueeRepository.save(masquage);

        // Supprimée par tous ses participants (sans nouveau message depuis) : effacée de la base.
        List<ConversationMasquee> masquages = conversationMasqueeRepository.findByConversationId(conversationId);
        boolean supprimeeParTous = conv.getParticipants().stream().allMatch(p -> masquages.stream()
                .anyMatch(m -> m.getUtilisateur().getId().equals(p.getId())
                        && messagesVisibles(conv, m.getDateMasquage()).findAny().isEmpty()));
        if (supprimeeParTous) {
            effacer(conv);
        }

        // Autres appareils de l'appelant : la discussion disparaît aussi.
        ApresValidation.executer(() -> realtimeGateway.envoyerA(uid, "MESSAGE",
                Map.of("conversationId", conversationId, "conversationSupprimee", true)));
    }

    private Conversation conversationDuParticipant(String conversationId, String uid) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
        if (conv.getParticipants().stream().noneMatch(p -> p.getId().equals(uid))) {
            throw new ForbiddenException("Vous ne faites pas partie de cette conversation");
        }
        return conv;
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
            String destinataire = participant.getId();
            // Après validation : relu aussitôt par les clients, le message doit déjà être enregistré.
            ApresValidation.executer(() -> realtimeGateway.envoyerA(destinataire, "MESSAGE", dto));
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

    /** @param depuis date de suppression de la discussion par l'utilisateur (null : jamais supprimée) */
    private ConversationResponseDTO toConversationDto(Conversation c, String currentUserId, LocalDateTime depuis) {
        Utilisateur correspondant = c.getParticipants().stream()
                .filter(p -> !p.getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        // La collection n'est pas triée (ordre des identifiants) : le dernier
        // message est le plus récent par date d'envoi.
        MessageResponseDTO dernierMsg = messagesVisibles(c, depuis)
                .max(java.util.Comparator.comparing(MessageContact::getDateEnvoi,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .map(this::toMessageDto)
                .orElse(null);

        long nonLus = messagesVisibles(c, depuis)
                .filter(m -> m.getExpediteur() != null && !m.getExpediteur().getId().equals(currentUserId) && (m.getLu() == null || !m.getLu()))
                .count();

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
                .correspondantEnLigne(correspondant != null && realtimeGateway.estEnLigne(correspondant.getId()))
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
