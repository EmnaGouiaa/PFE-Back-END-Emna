package fsegs.pfebackendemnagouuiaa.service;

import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import org.springframework.security.access.AccessDeniedException;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Participants automatiques des réunions hebdomadaires : encadrant créateur + stagiaire.
 */
public final class WeeklyMeetingParticipantPolicy {

    public static final String ERROR_ONLY_SUPERVISOR_CAN_CREATE =
            "Seul un encadrant académique ou professionnel du stage peut planifier une réunion hebdomadaire.";
    public static final String ERROR_STAGIAIRE_REQUIRED =
            "Le stage doit avoir un stagiaire pour planifier une réunion hebdomadaire.";
    public static final String ERROR_PARTICIPANTS_LOCKED =
            "Les participants d'une réunion hebdomadaire ne peuvent pas être modifiés.";

    private WeeklyMeetingParticipantPolicy() {
    }

    public static void assertEncadrantSupervisorCreates(Utilisateur creator) {
        if (creator == null || !MeetingVisibilityRules.isSupervisor(creator)) {
            throw new AccessDeniedException(ERROR_ONLY_SUPERVISOR_CAN_CREATE);
        }
    }

    public static Set<Utilisateur> resolveParticipants(Stage stage, Utilisateur creator) {
        assertEncadrantSupervisorCreates(creator);
        if (stage == null || stage.getStagiaire() == null || stage.getStagiaire().getId() == null) {
            throw new IllegalArgumentException(ERROR_STAGIAIRE_REQUIRED);
        }

        Set<Utilisateur> participants = new LinkedHashSet<>();
        participants.add(creator);
        participants.add(stage.getStagiaire());
        MeetingInvitationRules.assertNoCompanyManagerInvitation(participants);
        return MeetingInvitationRules.retainEligibleParticipants(participants);
    }

    public static void assertParticipantsUnchanged(Reunion reunion, Set<Utilisateur> requestedParticipants) {
        if (requestedParticipants == null) {
            return;
        }
        Set<Long> expected = reunion.getParticipants() == null
                ? Set.of()
                : reunion.getParticipants().stream()
                .filter(Objects::nonNull)
                .map(Utilisateur::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> requested = requestedParticipants.stream()
                .filter(Objects::nonNull)
                .map(Utilisateur::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!expected.equals(requested)) {
            throw new IllegalArgumentException(ERROR_PARTICIPANTS_LOCKED);
        }
    }
}
