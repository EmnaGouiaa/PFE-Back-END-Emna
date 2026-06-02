package fsegs.pfebackendemnagouuiaa.service;

import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;

import java.util.Objects;
import java.util.Set;

/**
 * Règles de visibilité des réunions (liste / consultation), indépendantes du classement temporel.
 */
public final class MeetingVisibilityRules {

    public static final String ERROR_NOT_MEETING_CREATOR =
            "Accès refusé : seul l'encadrant créateur de la réunion peut consulter ou modifier cette réunion.";

    private MeetingVisibilityRules() {
    }

    public static boolean isSupervisor(Utilisateur utilisateur) {
        return utilisateur != null
                && (utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                || utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL);
    }

    public static boolean isSupervisorOfStage(Utilisateur utilisateur, Stage stage) {
        if (utilisateur == null || stage == null || utilisateur.getId() == null) {
            return false;
        }
        boolean academic = utilisateur.getRole() == Role.ENCADRANT_ACADEMIQUE
                && stage.getEncadrantAcademique() != null
                && Objects.equals(utilisateur.getId(), stage.getEncadrantAcademique().getId());
        boolean professional = utilisateur.getRole() == Role.ENCADRANT_PROFESSIONNEL
                && stage.getEncadrantProfessionnel() != null
                && Objects.equals(utilisateur.getId(), stage.getEncadrantProfessionnel().getId());
        return academic || professional;
    }

    public static boolean isMeetingParticipant(Reunion reunion, Utilisateur utilisateur) {
        if (reunion == null || utilisateur == null || utilisateur.getId() == null) {
            return false;
        }
        Set<Utilisateur> participants = reunion.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return false;
        }
        return participants.stream()
                .anyMatch(participant -> participant != null
                        && Objects.equals(participant.getId(), utilisateur.getId()));
    }

    public static boolean isMeetingCreator(Reunion reunion, Utilisateur utilisateur) {
        if (reunion == null || utilisateur == null || utilisateur.getId() == null) {
            return false;
        }
        if (reunion.getEncadrantCreateurId() != null) {
            return Objects.equals(reunion.getEncadrantCreateurId(), utilisateur.getId());
        }
        Stage stage = reunion.getStage();
        if (stage == null) {
            return false;
        }
        String creatorType = reunion.getTypeEncadrantCreateur() != null
                ? reunion.getTypeEncadrantCreateur().trim().toUpperCase()
                : "";
        if ("ACADEMIQUE".equals(creatorType)) {
            return stage.getEncadrantAcademique() != null
                    && Objects.equals(stage.getEncadrantAcademique().getId(), utilisateur.getId());
        }
        if ("PROFESSIONNEL".equals(creatorType)) {
            return stage.getEncadrantProfessionnel() != null
                    && Objects.equals(stage.getEncadrantProfessionnel().getId(), utilisateur.getId());
        }
        return false;
    }

    /**
     * Réunion hebdomadaire : uniquement le créateur (encadrant académique ou professionnel).
     * Réunion finale : encadrants du stage ou participants (création système).
     */
    public static boolean canSupervisorAccessMeeting(Utilisateur utilisateur, Reunion reunion, Stage stage) {
        if (!isSupervisor(utilisateur) || reunion == null) {
            return false;
        }
        if (reunion instanceof ReunionFinale) {
            return canSupervisorListFinalMeeting(utilisateur, reunion, stage);
        }
        return isSupervisorOfStage(utilisateur, stage) && isMeetingCreator(reunion, utilisateur);
    }

    /**
     * Encadrant : réunion finale du stage (les deux encadrants) ou participant explicite.
     */
    public static boolean canSupervisorListFinalMeeting(Utilisateur utilisateur, Reunion reunion, Stage stage) {
        if (!isSupervisor(utilisateur)) {
            return false;
        }
        if (isSupervisorOfStage(utilisateur, stage)) {
            return true;
        }
        return isMeetingParticipant(reunion, utilisateur);
    }

    /** @deprecated Utiliser {@link #canSupervisorAccessMeeting} pour les réunions hebdomadaires. */
    public static boolean canSupervisorListMeeting(Utilisateur utilisateur, Reunion reunion, Stage stage) {
        return canSupervisorAccessMeeting(utilisateur, reunion, stage);
    }
}
