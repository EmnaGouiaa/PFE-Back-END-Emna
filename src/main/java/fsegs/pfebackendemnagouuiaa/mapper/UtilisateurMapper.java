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

public class UtilisateurMapper {

    private UtilisateurMapper() {
    }

    public static Utilisateur toEntity(CreateUserRequest request) {
        Utilisateur utilisateur = instantiateByRole(request.getRole());
        populateCommonFields(utilisateur, request.getNom(), request.getPrenom(), request.getEmail(),
                request.getTelephone(), request.getMatricule(), request.getActif(), request.getNomFichierSignature(), request.getRole());
        return utilisateur;
    }

    public static void updateEntity(Utilisateur utilisateur, UpdateUserRequest request) {
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setActif(request.getActif());
        utilisateur.setNomFichierSignature(request.getNomFichierSignature());
        utilisateur.setRole(request.getRole());
    }

    public static UserResponse toResponse(Utilisateur utilisateur) {
        UserResponse response = new UserResponse();
        response.setId(utilisateur.getId());
        response.setNom(utilisateur.getNom());
        response.setPrenom(utilisateur.getPrenom());
        response.setEmail(utilisateur.getEmail());
        response.setTelephone(utilisateur.getTelephone());
        response.setAdresse(utilisateur.getAdresse());
        response.setActif(utilisateur.getActif());
        response.setNomFichierSignature(utilisateur.getNomFichierSignature());
        response.setRole(utilisateur.getRole());
        response.setMatricule(utilisateur.getMatricule());

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

    private static Utilisateur instantiateByRole(Role role) {
        if (role == null) {
            return new Utilisateur();
        }

        return switch (role) {
            case STAGIAIRE -> new Stagiaire();
            case ENCADRANT_ACADEMIQUE -> new EncadrantAcademique();
            case ENCADRANT_PROFESSIONNEL -> new EncadrantProfessionnel();
            case RESPONSABLE_ENTREPRISE -> new ResponsableEntreprise();
            case RESPONSABLE_SERVICE_STAGES -> new ResponsableServiceStages();
            default -> new Utilisateur();
        };
    }

    private static void populateCommonFields(
            Utilisateur utilisateur,
            String nom,
            String prenom,
            String email,
            String telephone,
            String matricule,
            Boolean actif,
            String nomFichierSignature,
            Role role
    ) {
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email);
        utilisateur.setTelephone(telephone);
        utilisateur.setActif(actif != null ? actif : true);
        utilisateur.setNomFichierSignature(nomFichierSignature);
        utilisateur.setMatricule(matricule);
        utilisateur.setRole(role);
    }
}
