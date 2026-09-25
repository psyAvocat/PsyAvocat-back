package com.psyavocat.controller;

import com.psyavocat.security.AuthenticatedUser;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur d'authentification et de test de session Firebase.
 * Fournit l'endpoint protégé `GET /me` pour valider la transmission du token.
 */
@RestController
public class AuthController {

    private final AuthenticationContext authenticationContext;

    public AuthController(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    /**
     * Endpoint protégé permettant de valider l'authentification Firebase.
     * Retourne l'identité et les autorisations sans exposer de secrets ou de tokens.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        AuthenticatedUser user = authenticationContext.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Utilisateur non authentifié"));

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("firebaseUid", user.getFirebaseUid());
        response.put("email", user.getEmail());
        response.put("userId", user.getUtilisateur() != null ? user.getUtilisateur().getId() : null);
        response.put("hasMetierProfile", user.hasMetierProfile());
        response.put("nom", user.getNom());
        response.put("prenom", user.getPrenom());
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }
}
