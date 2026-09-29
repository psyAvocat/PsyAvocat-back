-- =============================================================================
-- PsyAvocat - Script de peuplement SQL Réel et Idempotent pour MySQL
-- Aligné sur le diagramme UML officiel et le système de pondération
-- =============================================================================

USE psyavocat_db;

-- 1. Domaines
INSERT INTO domaines (id, nom, description)
SELECT 'dom-droit-famille', 'Droit Privé & des Personnes', 'Affaires familiales, successions, filiation, régimes matrimoniaux et protection des personnes vulnérables.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Droit Privé & des Personnes');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-droit-travail', 'Droit du Travail & Social', 'Relations de travail, contrats, licenciements, rupture conventionnelle, contentieux prud''homal et harcèlement.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Droit du Travail & Social');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-droit-foncier', 'Droit Immobilier & Foncier', 'Titres fonciers, litiges de propriété coutumière et moderne, baux d''habitation et commerciaux, copropriété et expulsions.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Droit Immobilier & Foncier');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-droit-penal', 'Droit Pénal & Libertés', 'Défense pénale d''urgence, garde à vue, plaintes pénales, infractions, escroqueries et comparutions judiciaires.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Droit Pénal & Libertés');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-droit-affaires', 'Droit des Affaires & Commercial', 'Droit OHADA, rédaction de contrats commerciaux, création de sociétés, recouvrement de créances et litiges entre associés.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Droit des Affaires & Commercial');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-psy-clinique', 'Psychologie Clinique & Thérapie', 'Prise en charge psychothérapeutique des troubles de l''humeur, angoisses, dépression et estime de soi.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Psychologie Clinique & Thérapie');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-psy-travail', 'Psychologie du Travail & Risques Psycho-sociaux', 'Accompagnement de l''épuisement professionnel, burn-out, surcharge mentale et souffrance au travail.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Psychologie du Travail & Risques Psycho-sociaux');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-psy-famille', 'Psychologie Familiale & Conjugale', 'Thérapie de couple, médiation familiale, résolution des conflits parentaux et gestion des ruptures affectives.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Psychologie Familiale & Conjugale');

INSERT INTO domaines (id, nom, description)
SELECT 'dom-psy-trauma', 'Psychotraumatologie & Deuil', 'Accompagnement du deuil, traumatismes psychologiques, événements de vie douloureux et travail de résilience.'
WHERE NOT EXISTS (SELECT 1 FROM domaines WHERE nom = 'Psychotraumatologie & Deuil');

-- 2. Spécialités
INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-famille', 'Droit de la famille & des personnes', 'Divorce, garde d''enfants, pension alimentaire, autorité parentale et successions patrimoniales.', 'dom-droit-famille'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Droit de la famille & des personnes');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-travail', 'Droit du travail & relations sociales', 'Licenciements abusifs, rupture conventionnelle, impayés de salaires et harcèlement moral au travail.', 'dom-droit-travail'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Droit du travail & relations sociales');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-foncier', 'Droit immobilier & contentieux foncier', 'Contestation de titres fonciers, litiges de bornage, baux commerciaux et litiges d''expulsion.', 'dom-droit-foncier'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Droit immobilier & contentieux foncier');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-penal', 'Droit pénal & défense criminelle', 'Assistance en garde à vue, défense correctionnelle, plaintes avec constitution de partie civile.', 'dom-droit-penal'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Droit pénal & défense criminelle');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-affaires', 'Droit des affaires & contentieux commercial', 'Contrats commerciaux OHADA, contentieux entre actionnaires et recouvrement forcé de créances.', 'dom-droit-affaires'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Droit des affaires & contentieux commercial');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-burnout', 'Gestion du stress, anxiété & burn-out', 'Épuisement professionnel, surcharge cognitive, anxiété de performance et équilibre vie pro/perso.', 'dom-psy-travail'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Gestion du stress, anxiété & burn-out');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-clinique', 'Psychothérapie clinique & troubles de l''humeur', 'Accompagnement de la dépression, des idées sombres, du repli sur soi et des angoisses diffuses.', 'dom-psy-clinique'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Psychothérapie clinique & troubles de l''humeur');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-couple', 'Thérapie de couple & médiation familiale', 'Rétablissement de la communication conjugale, surmonter l''infidélité ou préparer une séparation apaisée.', 'dom-psy-famille'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Thérapie de couple & médiation familiale');

INSERT INTO specialites (id, nom, description, domaine_id)
SELECT 'spec-trauma', 'Psychotraumatologie & gestion du deuil', 'Dépassement du stress post-traumatique, deuil traumatique et reconstruction après choc émotionnel.', 'dom-psy-trauma'
WHERE NOT EXISTS (SELECT 1 FROM specialites WHERE nom = 'Psychotraumatologie & gestion du deuil');

-- 3. Catégories de Besoin
INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-litige-travail', 'Litige professionnel & employeur', 'Conflit avec votre employeur, licenciement, heures impayées ou rupture de contrat.', 'AVOCAT', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Litige professionnel & employeur');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-conflit-famille', 'Conflit familial & divorce', 'Procédure de divorce, garde des enfants, pension alimentaire ou partage successoral.', 'AVOCAT', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Conflit familial & divorce');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-litige-foncier', 'Litige foncier & immobilier', 'Problème lié à un terrain, contestation de titre foncier, litige locatif ou menace d''expulsion.', 'AVOCAT', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Litige foncier & immobilier');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-affaire-penale', 'Affaire pénale & poursuite judiciaire', 'Plainte déposée contre vous ou par vous, garde à vue, convocation au tribunal ou infraction.', 'AVOCAT', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Affaire pénale & poursuite judiciaire');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-droit-affaires', 'Contrat d''affaires & contentieux commercial', 'Litiges entre associés, facture impayée, inexécution contractuelle ou création de société.', 'AVOCAT', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Contrat d''affaires & contentieux commercial');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-burnout', 'Épuisement professionnel & burn-out', 'Surcharge mentale au travail, fatigue extrême, démotivation profonde et sentiment d''impuissance.', 'PSYCHOLOGUE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Épuisement professionnel & burn-out');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-anxiete', 'Anxiété, angoisse & gestion du stress', 'Crises de panique, oppression thoracique, insomnies répétées et inquiétudes permanentes.', 'PSYCHOLOGUE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Anxiété, angoisse & gestion du stress');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-crise-couple', 'Crise de couple & relations familiales', 'Difficultés de dialogue avec le conjoint, tensions familiales continues ou douleur d''une rupture.', 'PSYCHOLOGUE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Crise de couple & relations familiales');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-depression', 'Dépression, tristesse & baisse de moral', 'Perte d''élan vital, sentiment de solitude, vide affectif et difficultés à accomplir les tâches simples.', 'PSYCHOLOGUE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Dépression, tristesse & baisse de moral');

INSERT INTO categories_besoin (id, nom, description, type_professionnel, actif)
SELECT 'cat-trauma', 'Traumatisme émotionnel & deuil', 'Souvenir douloureux qui hante le présent, choc après une perte d''un proche ou agression subie.', 'PSYCHOLOGUE', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories_besoin WHERE nom = 'Traumatisme émotionnel & deuil');

-- 4. Professionnels réels (Maître Sangaré, Maître Dubois, Dr. Lambert, etc.)
-- Utilisateurs
INSERT INTO utilisateurs (id, nom, prenom, email, telephone, date_inscription)
SELECT 'avocat-sangare', 'Sangaré', 'Mamadou', 'mamadou.sangare@psyavocat.com', '+223 76 12 34 56', '2023-01-15'
WHERE NOT EXISTS (SELECT 1 FROM utilisateurs WHERE id = 'avocat-sangare');

INSERT INTO professionnels (id, biographie, ville, adresse, mode_consultation, statut_validation, photo_url, note_moyenne, nombre_avis, en_ligne, langues)
SELECT 'avocat-sangare', 'Avocat inscrit au Barreau du Mali depuis 2012. Ancien lauréat du concours d''éloquence, j''assure avec rigueur et combativité la défense des salariés, chefs d''entreprise et familles.', 'Bamako', 'Rue 14, Porte 205, Quartier Badalabougou', 'HYBRIDE', 'VALIDE', 'https://images.unsplash.com/photo-1556157382-97eda2d62296?auto=format&fit=crop&w=400&q=80', 4.9, 48, TRUE, 'Français, Bambara, Anglais'
WHERE NOT EXISTS (SELECT 1 FROM professionnels WHERE id = 'avocat-sangare');

INSERT INTO avocats (id, numero_barreau)
SELECT 'avocat-sangare', 'ML-BMK-2012-042'
WHERE NOT EXISTS (SELECT 1 FROM avocats WHERE id = 'avocat-sangare');

-- Spécialités Maître Sangaré
INSERT INTO professionnel_specialites (professionnel_id, specialite_id)
SELECT 'avocat-sangare', 'spec-travail' WHERE NOT EXISTS (SELECT 1 FROM professionnel_specialites WHERE professionnel_id = 'avocat-sangare' AND specialite_id = 'spec-travail');
INSERT INTO professionnel_specialites (professionnel_id, specialite_id)
SELECT 'avocat-sangare', 'spec-foncier' WHERE NOT EXISTS (SELECT 1 FROM professionnel_specialites WHERE professionnel_id = 'avocat-sangare' AND specialite_id = 'spec-foncier');
INSERT INTO professionnel_specialites (professionnel_id, specialite_id)
SELECT 'avocat-sangare', 'spec-affaires' WHERE NOT EXISTS (SELECT 1 FROM professionnel_specialites WHERE professionnel_id = 'avocat-sangare' AND specialite_id = 'spec-affaires');

-- Tarifs Maître Sangaré
INSERT INTO tarifs_professionnel (id, titre, montant, devise, description, duree_minutes, actif, professionnel_id)
SELECT 'tarif-sangare-1', 'Consultation juridique initiale', 25000, 'XOF', 'Évaluation de la situation et première analyse juridique personnalisée.', 45, TRUE, 'avocat-sangare'
WHERE NOT EXISTS (SELECT 1 FROM tarifs_professionnel WHERE id = 'tarif-sangare-1');

INSERT INTO tarifs_professionnel (id, titre, montant, devise, description, duree_minutes, actif, professionnel_id)
SELECT 'tarif-sangare-2', 'Audit complet & négociation', 50000, 'XOF', 'Examen approfondi des contrats ou pièces juridiques avec préconisations écrites.', 60, TRUE, 'avocat-sangare'
WHERE NOT EXISTS (SELECT 1 FROM tarifs_professionnel WHERE id = 'tarif-sangare-2');
