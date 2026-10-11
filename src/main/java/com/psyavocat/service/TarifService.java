package com.psyavocat.service;

import com.psyavocat.dto.referentiel.TarifProfessionnelDTO;
import com.psyavocat.dto.referentiel.TarifRequest;

import java.util.List;

/**
 * Tarifs publiés par le professionnel connecté.
 * Les tarifs actifs sont exposés aux clients (fiche publique) et servent de référence
 * serveur au calcul du montant et de l'acompte des rendez-vous.
 */
public interface TarifService {

    /** Tous les tarifs du professionnel connecté (publiés ou non). */
    List<TarifProfessionnelDTO> getMesTarifs();

    TarifProfessionnelDTO creer(TarifRequest request);

    TarifProfessionnelDTO modifier(String id, TarifRequest request);

    TarifProfessionnelDTO changerPublication(String id, boolean actif);

    void supprimer(String id);
}
