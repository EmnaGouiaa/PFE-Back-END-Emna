package fsegs.pfebackendemnagouuiaa.service;

import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Règles d'éligibilité aux invitations de réunions de suivi.
 * Le responsable d'entreprise est exclu des participants.
 */
public final class MeetingInvitationRules {

    public static final String ERROR_COMPANY_MANAGER_NOT_INVITABLE =
            "Le responsable d'entreprise ne peut pas être invité à une réunion de suivi.";

    private MeetingInvitationRules() {
    }

    public static boolean isEligibleMeetingParticipant(Utilisateur utilisateur) {
        return utilisateur != null && utilisateur.getRole() != Role.RESPONSABLE_ENTREPRISE;
    }

    public static void assertNoCompanyManagerInvitation(Collection<Utilisateur> participants) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        boolean hasCompanyManager = participants.stream()
                .filter(Objects::nonNull)
                .anyMatch(user -> user.getRole() == Role.RESPONSABLE_ENTREPRISE);
        if (hasCompanyManager) {
            throw new IllegalArgumentException(ERROR_COMPANY_MANAGER_NOT_INVITABLE);
        }
    }

    public static Set<Utilisateur> retainEligibleParticipants(Collection<Utilisateur> participants) {
        if (participants == null || participants.isEmpty()) {
            return Set.of();
        }
        return participants.stream()
                .filter(Objects::nonNull)
                .filter(MeetingInvitationRules::isEligibleMeetingParticipant)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
