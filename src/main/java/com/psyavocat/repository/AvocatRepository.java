package com.psyavocat.repository;

import com.psyavocat.entity.Avocat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvocatRepository extends JpaRepository<Avocat, String> {

    List<Avocat> findByStatutValidation(String statutValidation);

    /**
     * Un autre avocat a-t-il déjà ce numéro ? Comparaison sans casse ni espaces, y compris
     * pour les numéros enregistrés avant leur normalisation.
     */
    @Query("select count(a) > 0 from Avocat a "
            + "where upper(replace(a.numeroBarreau, ' ', '')) = :numero and a.id <> :id")
    boolean numeroBarreauDejaUtilise(@Param("numero") String numeroNormalise, @Param("id") String id);
}
