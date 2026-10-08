package com.psyavocat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * Migration idempotente liée au profil client unifié ({@code Client}).
 *
 * Les colonnes {@code fiches_patient.patient_id} et {@code dossiers.justiciable_id}
 * référençaient strictement {@code patients} / {@code justiciables}. Elles référencent
 * désormais {@code utilisateurs} (Hibernate crée la nouvelle contrainte) : l'ancienne
 * contrainte, qui refuserait un client unifié, est supprimée ici.
 *
 * Exécutée à chaque démarrage, sans effet si les anciennes contraintes n'existent plus.
 * Limitée à MySQL (les bases de test H2 sont recréées à chaque exécution).
 */
@Slf4j
@Component
public class ClientAccountMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public ClientAccountMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }
        supprimerContraintesObsoletes("fiches_patient", "patient_id", "patients");
        supprimerContraintesObsoletes("dossiers", "justiciable_id", "justiciables");
    }

    private void supprimerContraintesObsoletes(String table, String colonne, String tableReferenceeObsolete) {
        List<String> contraintes = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? "
                        + "AND REFERENCED_TABLE_NAME = ?",
                String.class, table, colonne, tableReferenceeObsolete);

        for (String contrainte : contraintes) {
            // Noms issus d'information_schema (pas d'entrée utilisateur).
            jdbcTemplate.execute("ALTER TABLE `" + table + "` DROP FOREIGN KEY `" + contrainte + "`");
            log.info("Contrainte obsolète {} supprimée sur {}.{} (profil client unifié)", contrainte, table, colonne);
        }
    }

    private boolean isMySql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        } catch (Exception e) {
            log.warn("Impossible de déterminer le type de base de données : {}", e.getMessage());
            return false;
        }
    }
}
