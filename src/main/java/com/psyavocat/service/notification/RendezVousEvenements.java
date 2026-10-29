package com.psyavocat.service.notification;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Disponibilite;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.service.NotificationService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import com.psyavocat.service.support.Entites;

/**
 * Événements métier liés aux rendez-vous : notifications des parties concernées
 * (persistées + WebSocket + push) et diffusion des changements de créneau.
 *
 * Les rappels et notifications sont attachés au compte, pas à l'univers affiché.
 */
@Component
public class RendezVousEvenements {

    public static final String RESSOURCE_RENDEZ_VOUS = "RENDEZ_VOUS";

    /** Types des rappels planifiés (RendezVousReminderScheduler). */
    public static final String RAPPEL_VEILLE = "RAPPEL_RDV_24H";
    public static final String RAPPEL_JOUR = "RAPPEL_RDV_JOUR_J";
    public static final String RAPPEL_VISIO = "RAPPEL_VISIO";

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH'h'mm", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH'h'mm");

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
                nomClient(rdv) + (estVisio(rdv) ? " a réservé une visioconférence " : " a réservé un rendez-vous au cabinet ")
                        + quand(rdv) + ".");
    }

    public void rendezVousModifie(RendezVous rdv) {
        notifierClient(rdv, "RDV_MODIFIE", "Rendez-vous modifié",
                "Votre rendez-vous avec " + nomPraticien(rdv) + " est déplacé au " + quand(rdv) + ".");
        notifierPraticien(rdv, "RDV_MODIFIE", "Rendez-vous déplacé",
                nomClient(rdv) + " a déplacé son rendez-vous au " + quand(rdv) + ".");
    }

    /** Prévient l'autre partie de l'annulation. */
    public void rendezVousAnnule(RendezVous rdv, String annuleParId) {
        rendezVousAnnule(rdv, annuleParId, null);
    }

    /** Annulation par l'une des parties : l'autre est prévenue, avec le motif s'il est donné. */
    public void rendezVousAnnule(RendezVous rdv, String annuleParId, String motif) {
        boolean parLeClient = rdv.getPatient() != null && rdv.getPatient().getId().equals(annuleParId);
        String suite = motif != null ? " Motif : " + motif : "";
        if (parLeClient) {
            notifierPraticien(rdv, "RDV_ANNULE", "Rendez-vous annulé",
                    nomClient(rdv) + " a annulé le rendez-vous du " + quand(rdv) + "." + suite);
        } else {
            notifierClient(rdv, "RDV_ANNULE", "Rendez-vous annulé",
                    nomPraticien(rdv) + " a annulé votre rendez-vous du " + quand(rdv) + "." + suite
                            + " Vous pouvez choisir un autre créneau.");
        }
    }

    /** Annulation décidée par la plateforme (praticien suspendu ou refusé) : le client est prévenu. */
    public void rendezVousAnnuleParPlateforme(RendezVous rdv) {
        notifierClient(rdv, "RDV_ANNULE", "Rendez-vous annulé",
                "Votre rendez-vous du " + quand(rdv) + " avec " + nomPraticien(rdv)
                        + " est annulé : ce praticien n'est plus disponible sur PsyAvocat. "
                        + "L'acompte simulé est annulé.");
    }

    /**
     * Rappel environ un jour avant : chaque partie reçoit le nom de l'autre, le jour, l'heure
     * et la modalité (visioconférence ou cabinet).
     */
    public void rappelVeille(RendezVous rdv, LocalDateTime maintenant) {
        String quand = quandRelatif(rdv, maintenant);
        notifierClient(rdv, RAPPEL_VEILLE, "Rappel de rendez-vous",
                consultation(rdv) + " avec " + nomPraticien(rdv) + " " + quand + ".");
        notifierPraticien(rdv, RAPPEL_VEILLE, "Rappel de rendez-vous",
                consultation(rdv) + " avec " + nomClient(rdv) + " " + quand + ".");
    }

    /** Rappel dans les deux heures qui précèdent le rendez-vous. */
    public void rappelJour(RendezVous rdv, LocalDateTime maintenant) {
        String titre = (estVisio(rdv) ? "Visioconférence à " : "Rendez-vous à ") + heure(rdv);
        String quand = quandRelatif(rdv, maintenant);
        notifierClient(rdv, RAPPEL_JOUR, titre, consultation(rdv) + " avec " + nomPraticien(rdv) + " " + quand + ".");
        notifierPraticien(rdv, RAPPEL_JOUR, titre, consultation(rdv) + " avec " + nomClient(rdv) + " " + quand + ".");
    }

    /**
     * Visioconférence imminente : chaque partie est invitée à rejoindre la salle (lien joint
     * à la notification). Le professionnel sait avec quel client il a rendez-vous.
     */
    public void rappelVisio(RendezVous rdv, LocalDateTime maintenant) {
        long minutes = Math.max(1, (long) Math.ceil(
                java.time.Duration.between(maintenant, rdv.getDateHeure()).getSeconds() / 60.0));
        String titre = "Visioconférence dans " + minutes + " min";
        notifierClient(rdv, RAPPEL_VISIO, titre, "Votre visioconférence avec " + nomPraticien(rdv)
                + " commence à " + heure(rdv) + ". Rejoignez la salle depuis votre rendez-vous.");
        notifierPraticien(rdv, RAPPEL_VISIO, titre, "Votre visioconférence avec " + nomClient(rdv)
                + " commence à " + heure(rdv) + ". Rejoignez la salle depuis vos rendez-vous.");
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
        return Entites.reel(rdv.getProfessionnel()) instanceof Avocat ? "AVOCAT" : "PSYCHOLOGUE";
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

    /** « aujourd'hui à 10h30 », « demain à 10h30 » ou « le jeudi 30 octobre à 10h30 ». */
    private String quandRelatif(RendezVous rdv, LocalDateTime maintenant) {
        LocalDate jour = rdv.getDateHeure().toLocalDate();
        if (jour.equals(maintenant.toLocalDate())) {
            return "aujourd'hui à " + heure(rdv);
        }
        if (jour.equals(maintenant.toLocalDate().plusDays(1))) {
            return "demain à " + heure(rdv);
        }
        return "le " + quand(rdv);
    }

    private String heure(RendezVous rdv) {
        return rdv.getDateHeure().format(FORMAT_HEURE);
    }

    private static boolean estVisio(RendezVous rdv) {
        return "VISIO".equalsIgnoreCase(rdv.getMode());
    }

    private static String consultation(RendezVous rdv) {
        return estVisio(rdv) ? "Visioconférence" : "Rendez-vous au cabinet";
    }

    private String nomPraticien(RendezVous rdv) {
        Utilisateur p = rdv.getProfessionnel();
        if (p == null) {
            return "votre praticien";
        }
        String civilite = Entites.reel(p) instanceof Avocat ? "Me " : "Dr ";
        return civilite + p.getPrenom() + " " + p.getNom();
    }

    private String nomClient(RendezVous rdv) {
        Utilisateur c = rdv.getPatient();
        return c != null ? c.getPrenom() + " " + c.getNom() : "Un client";
    }
}
