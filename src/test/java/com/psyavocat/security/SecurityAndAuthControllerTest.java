package com.psyavocat.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Client;
import com.psyavocat.repository.UtilisateurRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityAndAuthControllerTest.TestAdminController.class)
class SecurityAndAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @Test
    @DisplayName("GET /health - Accès public sans authentification")
    void testPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("psyAvocat"));
    }

    @Test
    @DisplayName("GET /me - Refus 401 sans token Authorization")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /me - Refus 401 avec un token Firebase invalide")
    void testProtectedEndpointWithInvalidToken() throws Exception {
        FirebaseAuthException authException = mock(FirebaseAuthException.class);
        when(authException.getMessage()).thenReturn("Invalid Firebase token");
        when(firebaseAuth.verifyIdToken("invalid-token-xyz")).thenThrow(authException);

        mockMvc.perform(get("/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /me - Succès 200 avec token Firebase valide et profil MySQL existant")
    void testProtectedEndpointWithValidTokenAndMetierProfile() throws Exception {
        String uid = "firebase-uid-avocat-123";
        String email = "maitre.dupont@psyavocat.fr";
        String validToken = "valid-firebase-jwt-token";

        // Sauvegarde d'un avocat lié à cet UID dans MySQL (H2)
        Avocat avocat = new Avocat();
        avocat.setId(uid);
        avocat.setEmail(email);
        avocat.setNom("Dupont");
        avocat.setPrenom("Jean");
        avocat.setDateInscription(LocalDate.now());
        avocat.setNumeroBarreau("BAR-75001");
        utilisateurRepository.save(avocat);

        // Mock du décodage Firebase
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(uid);
        when(mockToken.getEmail()).thenReturn(email);
        when(mockToken.getClaims()).thenReturn(Collections.emptyMap());
        when(firebaseAuth.verifyIdToken(validToken)).thenReturn(mockToken);

        mockMvc.perform(get("/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.firebaseUid").value(uid))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.userId").value(uid))
                .andExpect(jsonPath("$.hasMetierProfile").value(true))
                .andExpect(jsonPath("$.nom").value("Dupont"))
                .andExpect(jsonPath("$.prenom").value("Jean"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[?(@ == 'ROLE_AVOCAT')]").exists())
                .andExpect(jsonPath("$.roles[?(@ == 'ROLE_PROFESSIONNEL')]").exists());
    }

    @Test
    @DisplayName("CORS - Requête preflight OPTIONS autorisée pour Angular Web (http://localhost:4200)")
    void testCorsPreflightForAngularWeb() throws Exception {
        mockMvc.perform(options("/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS - Flutter Web sur un port local quelconque autorisé")
    void testCorsPreflightForFlutterWebRandomPort() throws Exception {
        mockMvc.perform(options("/api/profil")
                        .header("Origin", "http://localhost:53712")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:53712"));
    }

    @Test
    @DisplayName("CORS - Requête preflight rejetée pour origine non autorisée")
    void testCorsPreflightRejectedForUntrustedOrigin() throws Exception {
        mockMvc.perform(options("/me")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 Forbidden - Rejet lorsque l'utilisateur n'a pas les droits requis")
    void testAccessDenied403() throws Exception {
        String uid = "uid-sans-privilege";
        String email = "simple@psyavocat.fr";
        String validToken = "token-simple-user";

        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(uid);
        when(mockToken.getEmail()).thenReturn(email);
        when(mockToken.getClaims()).thenReturn(Collections.emptyMap());
        when(firebaseAuth.verifyIdToken(validToken)).thenReturn(mockToken);

        // Appel d'un endpoint test sécurisé par @PreAuthorize("hasRole('ADMINISTRATEUR')")
        mockMvc.perform(get("/admin/test-privilege")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Accès refusé : privilèges insuffisants pour exécuter cette opération."));
    }

    @Test
    @DisplayName("Compte désactivé - 403 ACCOUNT_DISABLED sur l'API, /me reste accessible")
    void testDisabledAccount() throws Exception {
        String token = "token-client-desactive";
        Client client = saveClient("uid-client-desactive", "desactive@psyavocat.fr");
        client.setActif(false);
        utilisateurRepository.save(client);
        mockToken(token, client.getId(), client.getEmail(), true, Collections.emptyMap());

        mockMvc.perform(get("/api/profil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(FirebaseAuthenticationFilter.CODE_ACCOUNT_DISABLED));

        mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));
    }

    @Test
    @DisplayName("Client à l'e-mail non vérifié - 403 EMAIL_NOT_VERIFIED, /me indique emailVerified=false")
    void testClientWithUnverifiedEmail() throws Exception {
        String token = "token-client-non-verifie";
        Client client = saveClient("uid-client-non-verifie", "nonverifie@psyavocat.fr");
        mockToken(token, client.getId(), client.getEmail(), false, Collections.emptyMap());

        mockMvc.perform(get("/api/profil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(FirebaseAuthenticationFilter.CODE_EMAIL_NOT_VERIFIED));

        mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    @DisplayName("Client à l'e-mail vérifié - accès normal à l'API")
    void testClientWithVerifiedEmail() throws Exception {
        String token = "token-client-verifie";
        Client client = saveClient("uid-client-verifie", "verifie@psyavocat.fr");
        mockToken(token, client.getId(), client.getEmail(), true, Collections.emptyMap());

        mockMvc.perform(get("/api/profil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Custom claim Firebase 'role' ignoré : les rôles viennent uniquement de MySQL")
    void testRoleClaimIsIgnored() throws Exception {
        String token = "token-claim-admin";
        mockToken(token, "uid-sans-profil-claim", "claim@psyavocat.fr", true, Map.of("role", "ADMINISTRATEUR"));

        mockMvc.perform(get("/admin/test-privilege").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private Client saveClient(String uid, String email) {
        Client client = new Client();
        client.setId(uid);
        client.setEmail(email);
        client.setNom("Diarra");
        client.setPrenom("Ramla");
        client.setDateInscription(LocalDate.now());
        return utilisateurRepository.save(client);
    }

    private void mockToken(String token, String uid, String email, boolean emailVerified,
                           Map<String, Object> claims) throws FirebaseAuthException {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(uid);
        when(mockToken.getEmail()).thenReturn(email);
        when(mockToken.isEmailVerified()).thenReturn(emailVerified);
        when(mockToken.getClaims()).thenReturn(claims);
        when(firebaseAuth.verifyIdToken(token)).thenReturn(mockToken);
    }

    /**
     * Contrôleur interne de test pour valider le déclenchement de @PreAuthorize et RestAccessDeniedHandler.
     */
    @RestController
    static class TestAdminController {
        @GetMapping("/admin/test-privilege")
        @PreAuthorize("hasRole('ADMINISTRATEUR')")
        public String adminOnlyEndpoint() {
            return "ok";
        }
    }
}
