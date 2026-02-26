-- Autorise le type NEW_APPLICATION dans la table notifications.
-- À exécuter une seule fois si l'erreur "Data truncated for column 'type'" apparaît.
ALTER TABLE notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
