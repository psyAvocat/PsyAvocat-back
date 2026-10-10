package com.psyavocat.service.impl;

import com.psyavocat.dto.contenu.ContenuRequest;
import com.psyavocat.dto.contenu.ContenuResponseDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.MediaUrlResolver;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.ContenuRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.ContenuService;
import com.psyavocat.service.support.ClientAccounts;
import com.psyavocat.storage.service.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ContenuServiceImpl implements ContenuService {

    public static final String TYPE_ARTICLE = "ARTICLE";
    public static final String TYPE_CONSEIL = "CONSEIL";
    private static final int MOTS_PAR_MINUTE = 200;

    private final ContenuRepository contenuRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SpecialiteRepository specialiteRepository;
    private final AuthenticationContext authenticationContext;
    private final ImageStorageService imageStorageService;
    private final MediaUrlResolver mediaUrlResolver;
    private final RealtimeGateway realtimeGateway;

    public ContenuServiceImpl(ContenuRepository contenuRepository,
                              UtilisateurRepository utilisateurRepository,
                              SpecialiteRepository specialiteRepository,
                              AuthenticationContext authenticationContext,
                              ImageStorageService imageStorageService,
                              MediaUrlResolver mediaUrlResolver,
                              RealtimeGateway realtimeGateway) {
        this.contenuRepository = contenuRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.specialiteRepository = specialiteRepository;
        this.authenticationContext = authenticationContext;
        this.imageStorageService = imageStorageService;
        this.mediaUrlResolver = mediaUrlResolver;
        this.realtimeGateway = realtimeGateway;
    }

    // ------------------------------------------------------------------
    // Lecture (clients)
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ContenuResponseDTO> rechercher(String type, String q, String specialiteId, String tri) {
        // Les publications sont destinées aux clients (application mobile).
        // Les professionnels gèrent les leurs via /mes-contenus.
        exigerClient();
        String typeNormalise = typeValide(type);
        String texte = q != null && !q.isBlank() ? q.trim() : null;
        String specialite = specialiteId != null && !specialiteId.isBlank() ? specialiteId : null;

        Comparator<Contenu> ordre = "populaire".equalsIgnoreCase(tri)
                ? Comparator.comparing((Contenu c) -> c.getNombreVues() != null ? c.getNombreVues() : 0L).reversed()
                : Comparator.comparing(Contenu::getDatePublication, Comparator.nullsLast(Comparator.reverseOrder()));

        return contenuRepository.rechercherPublies(typeNormalise, texte, specialite).stream()
                .filter(c -> auteurPublieValide(c.getAuteur()))
                .sorted(ordre)
                .map(c -> toDto(c, false))
                .toList();
    }

    @Override
    public ContenuResponseDTO getContenu(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Contenu contenu = contenuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cette ressource n'est plus disponible."));

        boolean estAuteur = contenu.getAuteur() != null && uid.equals(contenu.getAuteur().getId());
        boolean publie = Boolean.TRUE.equals(contenu.getActif()) && auteurPublieValide(contenu.getAuteur());
        if (!estAuteur && !publie) {
            // Un contenu désactivé ou dont l'auteur n'est plus valide n'est jamais servi.
            throw new ResourceNotFoundException("Cette ressource n'est plus disponible.");
        }

        Utilisateur lecteur = utilisateurRepository.findById(uid).orElse(null);
        boolean estClient = ClientAccounts.isClient(lecteur);
        // Lecture : clients, auteur (édition) ou administrateur (modération).
        if (!estAuteur && !estClient && !(lecteur instanceof Administrateur)) {
            throw new ForbiddenException("Les publications sont consultables depuis l'application mobile.");
        }

        if (!estAuteur && estClient) {
            contenuRepository.incrementerVues(id);
        }
        return toDto(contenu, true);
    }

    // ------------------------------------------------------------------
    // Gestion (auteurs professionnels)
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ContenuResponseDTO> getMesContenus() {
        Professionnel auteur = auteurAutorise();
        return contenuRepository.findByAuteurIdOrderByDatePublicationDesc(auteur.getId()).stream()
                .map(c -> toDto(c, false))
                .toList();
    }

    @Override
    public ContenuResponseDTO creer(ContenuRequest request) {
        Professionnel auteur = auteurAutorise();

        Contenu contenu = new Contenu();
        contenu.setAuteur(auteur);
        // Le type découle du profil : un avocat publie un ARTICLE, un psychologue un CONSEIL.
        contenu.setType(typeDeLAuteur(auteur));
        appliquer(contenu, request, auteur);
        contenu.setDatePublication(LocalDateTime.now());
        contenu.setNombreVues(0L);

        Contenu saved = contenuRepository.save(contenu);
        signaler(saved, "CREE");
        return toDto(saved, true);
    }

    @Override
    public ContenuResponseDTO modifier(String id, ContenuRequest request) {
        Professionnel auteur = auteurAutorise();
        Contenu contenu = contenuDeLAuteur(id, auteur);
        appliquer(contenu, request, auteur);
        contenu.setDateModification(LocalDateTime.now());
        Contenu saved = contenuRepository.save(contenu);
        signaler(saved, "MODIFIE");
        return toDto(saved, true);
    }

    @Override
    public ContenuResponseDTO changerStatut(String id, boolean actif) {
        Professionnel auteur = auteurAutorise();
        Contenu contenu = contenuDeLAuteur(id, auteur);
        contenu.setActif(actif);
        contenu.setDateModification(LocalDateTime.now());
        Contenu saved = contenuRepository.save(contenu);
        signaler(saved, actif ? "PUBLIE" : "DESACTIVE");
        return toDto(saved, true);
    }

    @Override
    public ContenuResponseDTO televerserImage(String id, MultipartFile image) {
        Professionnel auteur = auteurAutorise();
        Contenu contenu = contenuDeLAuteur(id, auteur);

        if (contenu.getImageObjectKey() != null) {
            imageStorageService.deleteImage(contenu.getImageObjectKey());
        }
        var stored = imageStorageService.uploadImage(image, "contenus", contenu.getId());
        contenu.setImageObjectKey(stored.getObjectKey() != null ? stored.getObjectKey() : stored.getPublicId());
        contenu.setDateModification(LocalDateTime.now());

        Contenu saved = contenuRepository.save(contenu);
        signaler(saved, "MODIFIE");
        return toDto(saved, true);
    }

    @Override
    public ContenuResponseDTO supprimerImage(String id) {
        Professionnel auteur = auteurAutorise();
        Contenu contenu = contenuDeLAuteur(id, auteur);
        if (contenu.getImageObjectKey() == null) {
            return toDto(contenu, true);
        }
        imageStorageService.deleteImage(contenu.getImageObjectKey());
        contenu.setImageObjectKey(null);
        contenu.setDateModification(LocalDateTime.now());

        Contenu saved = contenuRepository.save(contenu);
        signaler(saved, "MODIFIE");
        return toDto(saved, true);
    }

    @Override
    public void supprimer(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));
        Contenu contenu = contenuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable"));

        boolean estAuteur = contenu.getAuteur() != null && uid.equals(contenu.getAuteur().getId());
        if (!estAuteur && !(utilisateur instanceof Administrateur)) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres publications");
        }
        if (contenu.getImageObjectKey() != null) {
            imageStorageService.deleteImage(contenu.getImageObjectKey());
        }
        contenuRepository.delete(contenu);
        signaler(contenu, "SUPPRIME");
    }

    // ------------------------------------------------------------------
    // Règles
    // ------------------------------------------------------------------

    /** Seul un avocat ou un psychologue validé et actif peut publier. */
    private Professionnel auteurAutorise() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));
        if (!(utilisateur instanceof Avocat) && !(utilisateur instanceof Psychologue)) {
            throw new ForbiddenException("Seuls les avocats (articles) et les psychologues (conseils) peuvent publier");
        }
        Professionnel pro = (Professionnel) utilisateur;
        if (!auteurPublieValide(pro)) {
            throw new ForbiddenException("Votre compte doit être validé pour publier");
        }
        return pro;
    }

    /** Seuls les comptes clients (patient / justiciable) consultent les listes publiques. */
    private void exigerClient() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid).orElse(null);
        if (!ClientAccounts.isClient(utilisateur)) {
            throw new ForbiddenException("Les publications sont consultables depuis l'application mobile.");
        }
    }

    private Contenu contenuDeLAuteur(String id, Professionnel auteur) {
        Contenu contenu = contenuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable"));
        if (contenu.getAuteur() == null || !auteur.getId().equals(contenu.getAuteur().getId())) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres publications");
        }
        return contenu;
    }

    private void appliquer(Contenu contenu, ContenuRequest request, Professionnel auteur) {
        contenu.setTitre(request.getTitre().trim());
        contenu.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        contenu.setContenu(request.getContenu());
        contenu.setActif(request.getActif() == null || request.getActif());
        contenu.setSpecialite(specialiteDuBonUnivers(request.getSpecialiteId(), auteur));
    }

    /** La catégorie doit être une spécialité du même univers que l'auteur. */
    private Specialite specialiteDuBonUnivers(String specialiteId, Professionnel auteur) {
        if (specialiteId == null || specialiteId.isBlank()) {
            return null;
        }
        Specialite specialite = specialiteRepository.findById(specialiteId)
                .orElseThrow(() -> new BadRequestException("Catégorie inconnue"));
        String attendu = auteur instanceof Avocat ? "AVOCAT" : "PSYCHOLOGUE";
        String reel = specialite.resolveTypeProfessionnel();
        if (reel != null && !attendu.equalsIgnoreCase(reel)) {
            throw new BadRequestException("Cette catégorie n'appartient pas à votre univers professionnel");
        }
        return specialite;
    }

    private boolean auteurPublieValide(Utilisateur auteur) {
        return auteur instanceof Professionnel pro
                && !Boolean.FALSE.equals(pro.getActif())
                && "APPROVED".equalsIgnoreCase(pro.getStatutValidation());
    }

    private String typeDeLAuteur(Professionnel auteur) {
        return auteur instanceof Avocat ? TYPE_ARTICLE : TYPE_CONSEIL;
    }

    private String typeValide(String type) {
        String normalise = type != null ? type.trim().toUpperCase() : "";
        if (!TYPE_ARTICLE.equals(normalise) && !TYPE_CONSEIL.equals(normalise)) {
            throw new BadRequestException("Type de contenu invalide (ARTICLE ou CONSEIL)");
        }
        return normalise;
    }

    /** Les clients connectés rafraîchissent leurs listes (création, modification, retrait). */
    private void signaler(Contenu contenu, String action) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", contenu.getId());
        payload.put("type", contenu.getType());
        payload.put("action", action);
        realtimeGateway.diffuser("CONTENU_MIS_A_JOUR", payload);
    }

    private ContenuResponseDTO toDto(Contenu c, boolean avecTexte) {
        Utilisateur auteur = c.getAuteur();
        return ContenuResponseDTO.builder()
                .id(c.getId())
                .type(c.getType())
                .titre(c.getTitre())
                .description(c.getDescription())
                .contenu(avecTexte ? c.getContenu() : null)
                .datePublication(c.getDatePublication())
                .dateModification(c.getDateModification())
                .actif(c.getActif())
                .auteurId(auteur != null ? auteur.getId() : null)
                .auteurNom(auteur != null ? auteur.getNom() : null)
                .auteurPrenom(auteur != null ? auteur.getPrenom() : null)
                .auteurPhotoUrl(auteur != null ? mediaUrlResolver.photoUrlOf(auteur) : null)
                .specialiteId(c.getSpecialite() != null ? c.getSpecialite().getId() : null)
                .specialiteNom(c.getSpecialite() != null ? c.getSpecialite().getNom() : null)
                .imageUrl(imageStorageService.getAccessUrl(c.getImageObjectKey()))
                .tempsLectureMinutes(tempsLecture(c.getContenu()))
                .nombreVues(c.getNombreVues())
                .build();
    }

    private Integer tempsLecture(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        int mots = texte.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(mots / (double) MOTS_PAR_MINUTE));
    }
}
