-- ============================================================
--  CORRECTION : Foreign Keys avec ON DELETE CASCADE
--  À exécuter dans phpMyAdmin → Base "unilearn" → Onglet SQL
-- ============================================================

-- ─── ÉTAPE 1 : Trouver les noms exacts des contraintes ──────
-- (facultatif, juste pour vérifier)
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'evaluation'
  AND TABLE_SCHEMA = 'unilearn';

-- ─── ÉTAPE 2 : Désactiver temporairement les FK checks ──────
SET FOREIGN_KEY_CHECKS = 0;

-- ─── ÉTAPE 3 : Corriger la table RENDU ──────────────────────
-- Supprimer l'ancienne contrainte (nom trouvé dans l'erreur)
ALTER TABLE `rendu` DROP FOREIGN KEY `FK_2A7F8EB9456C5646`;

-- Recréer avec ON DELETE CASCADE
ALTER TABLE `rendu`
    ADD CONSTRAINT `FK_2A7F8EB9456C5646`
    FOREIGN KEY (`evaluation_id`) REFERENCES `evaluation` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ─── ÉTAPE 4 : Corriger la table QUESTION ───────────────────
-- D'abord trouver le nom de la contrainte :
SELECT CONSTRAINT_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'question'
  AND REFERENCED_TABLE_NAME = 'evaluation'
  AND TABLE_SCHEMA = 'unilearn';

-- Remplace 'FK_QUESTION_EVAL' par le nom retourné ci-dessus
-- (exemple, à adapter selon le résultat de la requête ci-haut)
-- ALTER TABLE `question` DROP FOREIGN KEY `FK_QUESTION_EVAL`;
-- ALTER TABLE `question`
--     ADD CONSTRAINT `FK_QUESTION_EVAL`
--     FOREIGN KEY (`evaluation_id`) REFERENCES `evaluation` (`id`)
--     ON DELETE CASCADE
--     ON UPDATE CASCADE;

-- ─── ÉTAPE 5 : Réactiver les FK checks ──────────────────────
SET FOREIGN_KEY_CHECKS = 1;

-- ─── VÉRIFICATION FINALE ─────────────────────────────────────
-- Confirme que les contraintes ont bien ON DELETE CASCADE
SELECT
    TABLE_NAME,
    CONSTRAINT_NAME,
    DELETE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE UNIQUE_CONSTRAINT_SCHEMA = 'unilearn'
  AND CONSTRAINT_NAME IN ('FK_2A7F8EB9456C5646');
