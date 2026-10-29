package com.psyavocat.service.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Salles de visioconférence des rendez-vous (serveur Jitsi Meet, configurable).
 *
 * Chaque rendez-vous en visio a sa propre salle, au nom imprévisible (128 bits aléatoires) :
 * seuls le client et le professionnel, qui reçoivent le lien, peuvent la rejoindre.
 */
@Component
public class SallesVisio {

    private final String adresseServeur;
    private final SecureRandom alea = new SecureRandom();

    public SallesVisio(@Value("${psyavocat.visio.url:https://meet.ffmuc.net}") String adresseServeur) {
        this.adresseServeur = adresseServeur.trim().replaceAll("/+$", "");
    }

    /** Lien d'une nouvelle salle, propre à un rendez-vous. */
    public String nouvelleSalle() {
        byte[] octets = new byte[16];
        alea.nextBytes(octets);
        return adresseServeur + "/PsyAvocat-" + HexFormat.of().formatHex(octets);
    }
}
