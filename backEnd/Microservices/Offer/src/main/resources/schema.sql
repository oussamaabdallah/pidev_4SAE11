-- Si la table offer_applications existe déjà, ajouter la colonne contract_id (lien vers le contrat créé à l'acceptation) :
--   ALTER TABLE offer_applications ADD COLUMN contract_id BIGINT NULL;

-- Table notifications (colonne is_read pour éviter le mot réservé "read" en MySQL/MariaDB)
-- Si la table existe déjà avec type ENUM ou VARCHAR trop court, exécuter une fois :
--   ALTER TABLE notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
-- Si la table existe déjà avec la colonne "read", exécuter une fois :
--   ALTER TABLE notifications CHANGE COLUMN `read` is_read BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    offer_id BIGINT,
    question_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_notification_recipient (recipient_user_id),
    INDEX idx_notification_read (is_read),
    INDEX idx_notification_created (created_at)
);
