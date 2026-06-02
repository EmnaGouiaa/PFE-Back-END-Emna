package fsegs.pfebackendemnagouuiaa.mapper;

import fsegs.pfebackendemnagouuiaa.dto.CreateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UpdateUserRequest;
import fsegs.pfebackendemnagouuiaa.dto.UserResponse;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import fsegs.pfebackendemnagouuiaa.entities.EncadrantProfessionnel;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableEntreprise;
import fsegs.pfebackendemnagouuiaa.entities.ResponsableServiceStages;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Stagiaire;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper utilitaire statique pour la hiérarchie {@link Utilisateur} et les DTO
 * {@link CreateUserRequest}, {@link UpdateUserRequest}, {@link UserResponse}.
 * <p>
 * Utilisé par {@link fsegs.pfebackendemnagouuiaa.services.UtilisateurServiceImpl} (CRUD admin, profil
 * authentifié, masquage des champs sensibles selon le rôle).
 */
public class UtilisateurMapper {

    /** Champs autorisés en lecture profil pour tous les rôles (base commune). */
    private static final List<String> COMMON_PROFILE_FIELDS = List.of(
            "email",
            "nom",
            "prenom",
            "telephone",
            "adresse",
            "urlSignature"
    );

    private UtilisateurMapper() {
    }

    /**
     * Instancie la sous-classe {@link Utilisateur} selon le rôle et renseigne les champs communs.
     *
     * @param request requête de création
     * @return entité concrète (Stagiaire, EncadrantAcademique, etc.)
     */
    public static Utilisateur toEntity(CreateUserRequest request) {
        Utilisateur utilisateur = instantiateByRole(request.getRole());
        populateCommonFields(utilisateur, request.getNom(), request.getPrenom(), request.getEmail(),
                request.getTelephone(), request.getMatricule(), request.getActif(), request.getUrlSignature(), request.getRole());

        if (utilisateur instanceof Stagiaire stagiaire) {
            stagiaire.setNiveau(request.getNiveau());
        }

        return utilisateur;
    }

    /**
     * Met à jour les attributs modifiables d'un utilisateur existant (sans changer le type concret).
     *
     * @param utilisateur entité cible
     * @param request requête de mise à jour
     */
    public static void updateEntity(Utilisateur utilisateur, UpdateUserRequest request) {
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setActif(request.getActif());
        utilisateur.setUrlSignature(request.getUrlSignature());
        // Le role n'est jamais modifie via update (regle metier : role fixe apres creation).
    }

    /**
     * Construit une réponse administrateur complète, avec champs spécifiques selon le type réel.
     *
     * @param utilisateur entité source
     * @return DTO exposé à l'API admin
     */
    public static UserResponse toResponse(Utilisateur utilisateur) {
        UserResponse response = new UserResponse();
        response.setId(utilisateur.getId());
        response.setNom(utilisateur.getNom());
        response.setPrenom(utilisateur.getPrenom());
        response.setEmail(utilisateur.getEmail());
        response.setTelephone(utilisateur.getTelephone());
        response.setAdresse(utilisateur.getAdresse());
        response.setActif(utilisateur.getActif());
        response.setUrlSignature(utilisateur.getUrlSignature());
        response.setRole(utilisateur.getRole());
        response.setMatricule(utilisateur.getMatricule());
        response.setDoitChangerMotDePasse(utilisateur.getDoitChangerMotDePasse());

        if (utilisateur instanceof Stagiaire stagiaire) {
            response.setDateNaiss(stagiaire.getDateNaiss());
            response.setNiveau(stagiaire.getNiveau());
            response.setFiliereId(stagiaire.getFiliere() != null ? stagiaire.getFiliere().getId() : null);
            response.setFiliereNom(stagiaire.getFiliere() != null ? stagiaire.getFiliere().getNom() : null);
        }

        if (utilisateur instanceof EncadrantAcademique encadrantAcademique) {
            response.setGrade(encadrantAcademique.getGrade());
            response.setMatricule(encadrantAcademique.getMatricule());
            response.setSpecialite(encadrantAcademique.getSpecialite());
        }

        if (utilisateur instanceof EncadrantProfessionnel encadrantProfessionnel) {
            response.setPoste(encadrantProfessionnel.getPoste());
            response.setService(encadrantProfessionnel.getService());
            response.setEntrepriseId(encadrantProfessionnel.getEntreprise() != null ? encadrantProfessionnel.getEntreprise().getId() : null);
            response.setEntrepriseNom(encadrantProfessionnel.getEntreprise() != null ? encadrantProfessionnel.getEntreprise().getNom() : null);
        }

        if (utilisateur instanceof ResponsableEntreprise responsableEntreprise) {
            response.setPoste(responsableEntreprise.getPoste());
            response.setService(responsableEntreprise.getService());
            response.setEntrepriseId(responsableEntreprise.getEntreprise() != null ? responsableEntreprise.getEntreprise().getId() : null);
            response.setEntrepriseNom(responsableEntreprise.getEntreprise() != null ? responsableEntreprise.getEntreprise().getNom() : null);
        }

        if (utilisateur instanceof ResponsableServiceStages responsableServiceStages) {
            response.setService(responsableServiceStages.getService());
        }

        return response;
    }

    /**
     * Réponse profil : masque identifiants internes et ne conserve que les champs autorisés par rôle.
     *
     * @param utilisateur utilisateur authentifié ou cible du profil
     * @return DTO filtré avec {@code champsProfilAutorises} et {@code documentsStageAutorises}
     */
    public static UserResponse toProfileResponse(Utilisateur utilisateur) {
        UserResponse response = toResponse(utilisateur);
        response.setId(null);
        response.setActif(null);
        response.setEntrepriseId(null);
        response.setEntrepriseNom(null);
        response.setFiliereId(null);
        response.setFiliereNom(null);
        response.setNiveau(null);

        List<String> allowedFields = new ArrayList<>(COMMON_PROFILE_FIELDS);
        if (utilisateur.getRole() != null) {
            switch (utilisateur.getRole()) {
                case STAGIAIRE -> allowedFields.addAll(List.of("dateNaiss", "matricule"));
                case RESPONSABLE_ENTREPRISE, ENCADRANT_PROFESSIONNEL -> allowedFields.addAll(List.of("poste", "service"));
                case ENCADRANT_ACADEMIQUE -> allowedFields.addAll(List.of("grade", "specialite"));
                case RESPONSABLE_STAGE -> allowedFields.addAll(List.of("service"));
                default -> {
                }
            }
        }
        response.setChampsProfilAutorises(allowedFields);
        response.setDocumentsStageAutorises(getDocumentsStageAutorises(utilisateur));

        // Annule les champs non listés dans champsProfilAutorises (évite la fuite d'infos admin)
        if (!allowedFields.contains("matricule")) {
            response.setMatricule(null);
        }
        if (!allowedFields.contains("grade")) {
            response.setGrade(null);
        }
        if (!allowedFields.contains("specialite")) {
            response.setSpecialite(null);
        }
        if (!allowedFields.contains("poste")) {
            response.setPoste(null);
        }
        if (!allowedFields.contains("service")) {
            response.setService(null);
        }
        if (!allowedFields.contains("dateNaiss")) {
            response.setDateNaiss(null);
        }

        return response;
    }

    /**
     * Types de documents de stage accessibles selon le rôle (règle métier front / téléchargement).
     */
    private static List<String> getDocumentsStageAutorises(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null) {
            return List.of();
        }

        return switch (utilisateur.getRole()) {
            case STAGIAIRE, ENCADRANT_PROFESSIONNEL -> List.of("CONVENTION", "FICHE_EVALUATION", "CAHIER_STAGE");
            case ENCADRANT_ACADEMIQUE, RESPONSABLE_STAGE -> List.of("CONVENTION", "CAHIER_STAGE");
            case RESPONSABLE_ENTREPRISE -> List.of("CONVENTION", "FICHE_EVALUATION", "CAHIER_STAGE");
            default -> List.of();
        };
    }

    /**
     * Fabrique la sous-classe JPA adaptée au {@link Role} ; {@link Utilisateur} générique si rôle inconnu.
     */
    private static Utilisateur instantiateByRole(Role role) {
        if (role == null) {
            return new Utilisateur();
        }

        return switch (role) {
            case STAGIAIRE -> new Stagiaire();
            case ENCADRANT_ACADEMIQUE -> new EncadrantAcademique();
            case ENCADRANT_PROFESSIONNEL -> new EncadrantProfessionnel();
            case RESPONSABLE_ENTREPRISE -> new ResponsableEntreprise();
            case RESPONSABLE_STAGE -> new ResponsableServiceStages();
            default -> new Utilisateur();
        };
    }

    /**
     * Renseigne les attributs communs à tous les types d'utilisateur.
     */
    private static void populateCommonFields(
            Utilisateur utilisateur,
            String nom,
            String prenom,
            String email,
            String telephone,
            String matricule,
            Boolean actif,
            String urlSignature,
            Role role
    ) {
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email);
        utilisateur.setTelephone(telephone);
        utilisateur.setActif(actif != null ? actif : true);
        utilisateur.setUrlSignature(urlSignature);
        utilisateur.setMatricule(matricule);
        utilisateur.setRole(role);
    }
}
