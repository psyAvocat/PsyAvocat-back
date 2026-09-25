package com.psyavocat.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.psyavocat.entity.Avocat;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
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
