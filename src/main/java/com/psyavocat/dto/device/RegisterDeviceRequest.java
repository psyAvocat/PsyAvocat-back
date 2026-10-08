package com.psyavocat.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Enregistrement (ou renouvellement) du token FCM de l'appareil courant. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceRequest {

    @NotBlank(message = "Le token de l'appareil est obligatoire")
    @Size(max = 512, message = "Token d'appareil invalide")
    private String token;

    @NotBlank(message = "La plateforme est obligatoire")
    @Pattern(regexp = "ANDROID|IOS|WEB", message = "Plateforme invalide (ANDROID, IOS ou WEB)")
    private String plateforme;
}
