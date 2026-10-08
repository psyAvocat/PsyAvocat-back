package com.psyavocat.service.notification;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Disponibilite;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.service.NotificationService;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Événements métier liés aux rendez-vous : notifications des parties concernées
 * (persistées + WebSocket + push) et diffusion des changements de créneau.
 *
 * Les rappels et notifications sont attachés au compte, pas à l'univers affiché.
 */
@Component
public class RendezVousEvenements {

    public static final String RESSOURCE_RENDEZ_VOUS = "RENDEZ_VOUS";

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH'h'mm", Locale.FRENCH);

    private final NotificationService notificationService;
    private final RealtimeGateway realtimeGateway;

    public RendezVousEvenements(NotificationService notificationService, RealtimeGateway realtimeGateway) {
        this.notificationService = notificationService;
        this.realtimeGateway = realtimeGateway;
    }

    public void rendezVousConfirme(RendezVous rdv) {
        notifierClient(rdv, "RDV_CONFIRME", "Rendez-vous confirmé",
                "Votre rendez-vous avec " + nomPraticien(rdv) + " est confirmé pour " + quand(rdv) + ".");
        notifierPraticien(rdv, "RDV_NOUVEAU", "Nouveau rendez-vous",
                nomClient(rdv) + " a réservé un rendez-vous " + quand(rdv) + ".");
    }

    public void rendezVousModifie(RendezVous rdv) {
        notifierClient(rdv, "RDV_MODIFIE", "Rendez-vous modifié",
                "Votre rendez-vous avec " + nomPraticien(rdv) + " est déplacé au " + quand(rdv) + ".");
        notifierPraticien(rdv, "RDV_MODIFIE", "Rendez-vous déplacé",
                nomClient(rdv) + " a déplacé son rendez-vous au " + quand(rdv) + ".");
    }

    /** Prévient l'autre partie de l'annulation. */
    public void rendezVousAnnule(RendezVous rdv, String annuleParId) {
        boolean parLeClient = rdv.getPatient() != null && rdv.getPatient().getId().equals(annuleParId);
        if (parLeClient) {
            notifierPraticien(rdv, "RDV_ANNULE", "Rendez-vous annulé",
                    nomClient(rdv) + " a annulé le rendez-vous du " + quand(rdv) + ".");
        } else {
            notifierClient(rdv, "RDV_ANNULE", "Rendez-vous annulé",
                    nomPraticien(rdv) + " a annulé votre rendez-vous du " + quand(rdv) + ".");
        }
    }

    public void rappel(RendezVous rdv, String type, String titre, String message) {
        notifierClient(rdv, type, titre, message);
        notifierPraticien(rdv, type, titre, message);
    }

    /** Diffuse le nouvel état d'un créneau (aucune donnée personnelle). */
    public void creneauMisAJour(Disponibilite d) {
        if (d == null || d.getProfessionnel() == null) {
            return;
        }
        realtimeGateway.diffuser("CRENEAU_MIS_A_JOUR", Map.of(
                "disponibiliteId", d.getId(),
                "professionnelId", d.getProfessionnel().getId(),
                "statut", d.getStatut()));
    }

    /** AVOCAT ou PSYCHOLOGUE, selon le praticien du rendez-vous. */
    public static String universDe(RendezVous rdv) {
        return rdv.getProfessionnel() instanceof Avocat ? "AVOCAT" : "PSYCHOLOGUE";
    }

    private void notifierClient(RendezVous rdv, String type, String titre, String message) {
        if (rdv.getPatient() != null) {
            notifier(rdv.getPatient(), rdv, type, titre, message);
        }
    }

    private void notifierPraticien(RendezVous rdv, String type, String titre, String message) {
        if (rdv.getProfessionnel() != null) {
            notifier(rdv.getProfessionnel(), rdv, type, titre, message);
        }
    }

    private void notifier(Utilisateur destinataire, RendezVous rdv, String type, String titre, String message) {
        notificationService.notifier(new NotificationEvenement(
                destinataire.getId(), type, titre, message, universDe(rdv),
                RESSOURCE_RENDEZ_VOUS, rdv.getId(), rdv.getLienVisio()));
    }

    private String quand(RendezVous rdv) {
        return rdv.getDateHeure() != null ? rdv.getDateHeure().format(FORMAT_DATE) : "";
    }

    private String nomPraticien(RendezVous rdv) {
        Utilisateur p = rdv.getProfessionnel();
        if (p == null) {
            return "votre praticien";
        }
        String civilite = p instanceof Avocat ? "Me " : "Dr ";
        return civilite + p.getPrenom() + " " + p.getNom();
    }

    private String nomClient(RendezVous rdv) {
        Utilisateur c = rdv.getPatient();
        return c != null ? c.getPrenom() + " " + c.getNom() : "Un client";
    }
}
