package com.psyavocat.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Client;
import com.psyavocat.entity.Disponibilite;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.repository.DisponibiliteRepository;
import com.psyavocat.repository.UtilisateurRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrôle du statut professionnel et des accès anonymes, de bout en bout
 * (filtre Firebase → contrôleur → service → H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccesProfessionnelSecurityTest {

    private static final String CODE_NON_APPROUVE = "PROFESSIONAL_NOT_APPROVED";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private DisponibiliteRepository disponibiliteRepository;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    // ------------------------------------------------------------------
    // Endpoints anonymes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Espace professionnel (clients, stats) : 401 sans authentification")
    void espacePersonnel_anonyme_401() throws Exception {
        mockMvc.perform(get("/api/professionnels/clients")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/professionnels/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/professionnel/clients")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/professionnel/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Catalogue public toujours accessible anonymement")
    void catalogue_anonyme_200() throws Exception {
        mockMvc.perform(get("/api/professionnels")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Statut de vérification
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PENDING : création de créneau refusée (403 + code), profil consultable")
    void pending_creneauRefuse_profilAutorise() throws Exception {
        String token = connecter(avocat("PENDING"));

        mockMvc.perform(post("/api/disponibilites")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creneauJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CODE_NON_APPROUVE));

        mockMvc.perform(get("/api/profil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PENDING : dossiers reçus, clients et statistiques refusés")
    void pending_fonctionsMetierRefusees() throws Exception {
        String token = connecter(avocat("PENDING"));

        mockMvc.perform(get("/api/avocat/soumissions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CODE_NON_APPROUVE));
        mockMvc.perform(get("/api/professionnels/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/professionnel/stats").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUSPENDED : séances et fiches patient refusées")
    void suspendu_seancesRefusees() throws Exception {
        String token = connecter(psychologue("SUSPENDED"));

        mockMvc.perform(get("/api/psychologue/seances").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CODE_NON_APPROUVE));
        mockMvc.perform(get("/api/psychologue/fiches-patient").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REJECTED : dépôt de justificatif refusé")
    void refuse_justificatifRefuse() throws Exception {
        String token = connecter(avocat("REJECTED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/justificatifs")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "diplome.pdf", "application/pdf", new byte[]{1, 2, 3}))
                        .param("typeDocument", "DIPLOME")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CODE_NON_APPROUVE));
    }

    @Test
    @DisplayName("APPROVED : clients et création de créneau autorisés")
    void approuve_accesMetier() throws Exception {
        String token = connecter(avocat("APPROVED"));

        mockMvc.perform(get("/api/professionnels/clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/disponibilites")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creneauJson()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Créneaux d'un professionnel suspendu jamais proposés publiquement")
    void creneauxPublics_professionnelSuspendu_vides() throws Exception {
        Professionnel pro = psychologue("SUSPENDED");
        Disponibilite d = new Disponibilite();
        d.setDate(LocalDate.now().plusDays(3));
        d.setHeureDebut(LocalTime.of(10, 0));
        d.setHeureFin(LocalTime.of(11, 0));
        d.setStatut("LIBRE");
        d.setProfessionnel(pro);
        disponibiliteRepository.save(d);

        mockMvc.perform(get("/api/disponibilites/professionnel/" + pro.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------
    // Messagerie
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Messagerie : un professionnel approuvé ne peut pas écrire à un client sans relation")
    void messagerie_professionnelVersClientInconnu_403() throws Exception {
        String token = connecter(avocat("APPROVED"));
        Client inconnu = client();

        mockMvc.perform(post("/api/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinataireId\":\"" + inconnu.getId() + "\",\"premierMessage\":\"Bonjour\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Messagerie : un client ne peut pas écrire à un professionnel en attente")
    void messagerie_clientVersProfessionnelEnAttente_403() throws Exception {
        Professionnel enAttente = psychologue("PENDING");
        String token = connecter(client());

        mockMvc.perform(post("/api/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinataireId\":\"" + enAttente.getId() + "\",\"premierMessage\":\"Bonjour\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Réservation directe d'un avocat (contournement du dossier) : endpoint retiré")
    void reservationDirecteAvocat_retiree() throws Exception {
        Professionnel avocat = avocat("APPROVED");
        String token = connecter(client());

        mockMvc.perform(post("/api/rendez-vous/avocat/direct")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avocatId\":\"" + avocat.getId() + "\",\"disponibiliteId\":\"x\",\"montantTotal\":100}"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Séances
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Séance : un psychologue ne peut pas créer de fiche pour un client qui n'est pas son patient")
    void seance_clientSansRelation_403() throws Exception {
        String token = connecter(psychologue("APPROVED"));
        Client inconnu = client();

        mockMvc.perform(post("/api/psychologue/seances")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + inconnu.getId() + "\",\"date\":\"2026-01-10T10:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Données de test
    // ------------------------------------------------------------------

    private String creneauJson() {
        return "{\"date\":\"" + LocalDate.now().plusDays(5) + "\",\"heureDebut\":\"10:00:00\",\"heureFin\":\"11:00:00\"}";
    }

    private Avocat avocat(String statut) {
        Avocat a = new Avocat();
        remplir(a);
        a.setNumeroBarreau("BAR-" + a.getId());
        a.setStatutValidation(statut);
        return utilisateurRepository.save(a);
    }

    private Psychologue psychologue(String statut) {
        Psychologue p = new Psychologue();
        remplir(p);
        p.setNumeroAgrement("AGR-" + p.getId());
        p.setStatutValidation(statut);
        return utilisateurRepository.save(p);
    }

    private Client client() {
        Client c = new Client();
        remplir(c);
        return utilisateurRepository.save(c);
    }

    private void remplir(com.psyavocat.entity.Utilisateur u) {
        String id = UUID.randomUUID().toString();
        u.setId(id);
        u.setEmail(id + "@test.psyavocat");
        u.setNom("Nom");
        u.setPrenom("Prenom");
        u.setDateInscription(LocalDate.now());
    }

    /** Simule un jeton Firebase valide (e-mail vérifié) pour cet utilisateur. */
    private String connecter(com.psyavocat.entity.Utilisateur u) throws Exception {
        String token = "token-" + u.getId();
        FirebaseToken decoded = mock(FirebaseToken.class);
        when(decoded.getUid()).thenReturn(u.getId());
        when(decoded.getEmail()).thenReturn(u.getEmail());
        when(decoded.isEmailVerified()).thenReturn(true);
        when(decoded.getClaims()).thenReturn(Collections.emptyMap());
        when(firebaseAuth.verifyIdToken(token)).thenReturn(decoded);
        return token;
    }
}
