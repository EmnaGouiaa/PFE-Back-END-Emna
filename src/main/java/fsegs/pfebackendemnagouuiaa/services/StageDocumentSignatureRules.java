package fsegs.pfebackendemnagouuiaa.services;



import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;

import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;

import fsegs.pfebackendemnagouuiaa.entities.Stage;

import fsegs.pfebackendemnagouuiaa.entities.StatutStage;

import fsegs.pfebackendemnagouuiaa.exception.BusinessException;



/**

 * Regles metier pour autoriser ou refuser une signature (hors PDF).

 */

public final class StageDocumentSignatureRules {



    private static final String LOGBOOK_SIGN_BEFORE_END_MESSAGE =

            "La signature du cahier de stage n'est autorisee qu'apres la date de fin du stage.";



    private static final String CONVENTION_STAGE_UPCOMING_MESSAGE =

            "La signature de la convention n'est pas disponible tant que le stage est a venir.";



    private static final String CONVENTION_STAGE_STATUS_MESSAGE =

            "La signature de la convention n'est disponible que pour un stage en cours ou termine.";



    private StageDocumentSignatureRules() {

    }



    /**

     * Convention : signable uniquement si le stage est {@link StatutStage#EN_COURS}

     * ou {@link StatutStage#TERMINE}.

     */

    public static void ensureConventionSigningAllowed(Stage stage, ConventionStage convention) {

        if (stage == null) {

            throw new BusinessException("Stage introuvable.");

        }

        if (convention == null) {

            throw new BusinessException("Convention introuvable.");

        }

        if (stage.getStatut() == StatutStage.REFUSE || stage.getStatut() == StatutStage.ANNULE) {

            throw new BusinessException("La convention n'est pas disponible pour un stage refuse ou annule.");

        }

        if (stage.getStatut() == StatutStage.A_VENIR || stage.getStatut() == StatutStage.PAS_COMMENCE) {

            throw new BusinessException(CONVENTION_STAGE_UPCOMING_MESSAGE);

        }

        if (stage.getStatut() != StatutStage.EN_COURS && stage.getStatut() != StatutStage.TERMINE) {

            throw new BusinessException(CONVENTION_STAGE_STATUS_MESSAGE);

        }

        if (convention.estCompletementSigne()) {

            throw new BusinessException("La convention est deja entierement signee.");

        }

    }



    public static void ensureLogbookSigningAllowed(Stage stage) {

        if (!FinalStageDocumentPdfAccessService.isStageEndDateReached(stage)) {

            throw new BusinessException(LOGBOOK_SIGN_BEFORE_END_MESSAGE);

        }

    }



    public static void ensureEvaluationSigningAllowed(FicheEvaluation fiche, boolean rolePartComplete) {

        if (fiche == null) {

            throw new BusinessException("La fiche d'evaluation est introuvable.");

        }

        if (!rolePartComplete) {

            throw new BusinessException(

                    "La fiche d'evaluation doit etre entierement renseignee avant la signature.");

        }

    }

}


