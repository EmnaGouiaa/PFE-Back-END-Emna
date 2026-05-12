CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    body VARCHAR(1200) NOT NULL,
    date_creation DATETIME NOT NULL,
    type_notification VARCHAR(255) NOT NULL,
    stage_id BIGINT NULL,
    reunion_id BIGINT NULL,
    reunion_finale_id BIGINT NULL,
    reunion_hebdomadaire_id BIGINT NULL,
    cahier_stage_id BIGINT NULL,
    fiche_evaluation_id BIGINT NULL,
    convention_id BIGINT NULL,
    demande_id BIGINT NULL,
    parent_notification_id BIGINT NULL,
    CONSTRAINT fk_notification_stage
        FOREIGN KEY (stage_id) REFERENCES stage(id),
    CONSTRAINT fk_notification_reunion
        FOREIGN KEY (reunion_id) REFERENCES reunion(id),
    CONSTRAINT fk_notification_reunion_finale
        FOREIGN KEY (reunion_finale_id) REFERENCES reunion(id),
    CONSTRAINT fk_notification_reunion_hebdomadaire
        FOREIGN KEY (reunion_hebdomadaire_id) REFERENCES reunion(id),
    CONSTRAINT fk_notification_cahier_stage
        FOREIGN KEY (cahier_stage_id) REFERENCES cahier_stage(id),
    CONSTRAINT fk_notification_fiche_evaluation
        FOREIGN KEY (fiche_evaluation_id) REFERENCES fiche_evaluation(id),
    CONSTRAINT fk_notification_convention
        FOREIGN KEY (convention_id) REFERENCES conventions_stage(id),
    CONSTRAINT fk_notification_demande
        FOREIGN KEY (demande_id) REFERENCES demandes_stage(id),
    CONSTRAINT fk_notification_parent
        FOREIGN KEY (parent_notification_id) REFERENCES notification(id)
);

CREATE TABLE IF NOT EXISTS notification_destinataire (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    utilisateur_id BIGINT NOT NULL,
    statut_notification VARCHAR(50) NOT NULL,
    statut_action_notif VARCHAR(50) NOT NULL,
    date_lecture DATETIME NULL,
    date_action DATETIME NULL,
    reponse VARCHAR(2000) NULL,
    CONSTRAINT uk_notification_destinataire_notification_utilisateur
        UNIQUE (notification_id, utilisateur_id),
    CONSTRAINT fk_notification_destinataire_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id),
    CONSTRAINT fk_notification_destinataire_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id)
);

INSERT INTO notification (id, titre, body, date_creation, type_notification, parent_notification_id)
SELECT n.id,
       n.titre,
       COALESCE(n.message, ''),
       COALESCE(n.cree_le, NOW()),
       COALESCE(NULLIF(n.type, ''), 'REUNION_FIXEE'),
       NULL
FROM notifications n
WHERE NOT EXISTS (
    SELECT 1
    FROM notification nn
    WHERE nn.id = n.id
);

INSERT INTO notification_destinataire (
    id,
    notification_id,
    utilisateur_id,
    statut_notification,
    statut_action_notif,
    date_lecture,
    date_action,
    reponse
)
SELECT n.id,
       n.id,
       n.utilisateur_id,
       CASE WHEN COALESCE(n.lu, 0) = 1 THEN 'LUE' ELSE 'NON_LUE' END,
       'PENDING',
       n.lu_le,
       NULL,
       NULL
FROM notifications n
WHERE n.utilisateur_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM notification_destinataire nd
      WHERE nd.notification_id = n.id
        AND nd.utilisateur_id = n.utilisateur_id
  );
