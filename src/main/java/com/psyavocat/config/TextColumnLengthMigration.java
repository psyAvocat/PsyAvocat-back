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
 * Migration idempotente : élargit les colonnes de texte trop courtes pour ce que
 * l'API accepte (sinon MySQL refuse l'enregistrement : « Data too long for column »).
 *
 * <ul>
 *   <li>{@code contenus.description} : résumé d'article, jusqu'à 500 caractères ;</li>
 *   <li>{@code signalements.contenu_vise} : commentaire d'un signalement, jusqu'à 1 000 caractères.</li>
 * </ul>
 *
 * {@code ddl-auto=update} crée les colonnes mais n'élargit jamais une colonne existante :
 * la correction est faite ici, à chaque démarrage, sans effet si la colonne est déjà assez
 * large. Limitée à MySQL (les bases de test H2 sont recréées à chaque exécution).
 */
@Slf4j
@Component
public class TextColumnLengthMigration implements ApplicationRunner {

    private record Colonne(String table, String nom, int longueur, boolean obligatoire) {
    }

    private static final List<Colonne> COLONNES = List.of(
            new Colonne("contenus", "description", 500, false),
            new Colonne("signalements", "contenu_vise", 1000, false));

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public TextColumnLengthMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }
        COLONNES.forEach(this::elargirSiNecessaire);
    }

    private void elargirSiNecessaire(Colonne colonne) {
        List<Long> longueurs = jdbcTemplate.queryForList(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? "
                        + "AND DATA_TYPE = 'varchar'",
                Long.class, colonne.table(), colonne.nom());
        if (longueurs.isEmpty() || longueurs.get(0) == null || longueurs.get(0) >= colonne.longueur()) {
            return;
        }
        // Noms et longueurs fixés ci-dessus (pas d'entrée utilisateur).
        jdbcTemplate.execute("ALTER TABLE `" + colonne.table() + "` MODIFY `" + colonne.nom() + "` VARCHAR("
                + colonne.longueur() + ")" + (colonne.obligatoire() ? " NOT NULL" : " NULL"));
        log.info("Colonne {}.{} élargie de {} à {} caractères", colonne.table(), colonne.nom(),
                longueurs.get(0), colonne.longueur());
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
