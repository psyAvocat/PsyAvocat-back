package com.psyavocat.repository;

import com.psyavocat.entity.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, String> {

    Optional<DeviceRegistration> findByToken(String token);

    List<DeviceRegistration> findByUtilisateurIdAndActifTrue(String utilisateurId);

    List<DeviceRegistration> findByTokenIn(Collection<String> tokens);
}
