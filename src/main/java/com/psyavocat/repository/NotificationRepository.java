package com.psyavocat.repository;

import com.psyavocat.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByDestinataireIdOrderByDateEnvoiDesc(String destinataireId);

    long countByDestinataireIdAndLuFalse(String destinataireId);

    long countByDestinataireIdAndRessourceTypeAndLuFalse(String destinataireId, String ressourceType);

    List<Notification> findByDestinataireIdAndRessourceTypeAndLuFalse(String destinataireId, String ressourceType);

    List<Notification> findByDestinataireIdAndLuTrue(String destinataireId);

    List<Notification> findByDestinataireIdAndRessourceTypeAndRessourceIdAndLuFalse(
            String destinataireId, String ressourceType, String ressourceId);
}
