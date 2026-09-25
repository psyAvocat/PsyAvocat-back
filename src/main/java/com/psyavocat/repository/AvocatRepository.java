package com.psyavocat.repository;

import com.psyavocat.entity.Avocat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvocatRepository extends JpaRepository<Avocat, String> {

    List<Avocat> findByStatutValidation(String statutValidation);
}
