package com.psyavocat.repository;

import com.psyavocat.entity.Justiciable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JusticiableRepository extends JpaRepository<Justiciable, String> {
}
