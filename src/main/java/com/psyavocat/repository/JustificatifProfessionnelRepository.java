package com.psyavocat.repository;

import com.psyavocat.entity.JustificatifProfessionnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JustificatifProfessionnelRepository extends JpaRepository<JustificatifProfessionnel, String> {
    List<JustificatifProfessionnel> findByProfessionnelId(String professionnelId);

    List<JustificatifProfessionnel> findByProfessionnelIdAndTypeDocumentAndStatutValidation(
            String professionnelId, String typeDocument, String statutValidation);

    /** Documents en attente déposés par des professionnels d'un statut donné (ex. mises à jour des comptes validés). */
    List<JustificatifProfessionnel> findByStatutValidationAndProfessionnelStatutValidationOrderByDateDepotAsc(
            String statutValidation, String statutProfessionnel);
}
