package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.repository.StagiaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Genere automatiquement le matricule des stagiaires au format MAT + YY + NNNN
 * ou YY = 2 derniers chiffres de l'annee courante, NNNN = compteur incremental sur 4 chiffres.
 * Exemple : premier stagiaire de 2023 -> MAT230001, 2096e -> MAT232096.
 *
 * Le compteur reprend systematiquement a partir du plus grand suffixe existant pour
 * l'annee courante, garantissant unicite et continuite.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatriculeGeneratorService {

    public static final String MATRICULE_PREFIX = "MAT";
    private static final int COUNTER_WIDTH = 4;
    private static final int MAX_COUNTER = 9999;

    private final StagiaireRepository stagiaireRepository;

    /**
     * Construit le prochain matricule pour l'annee courante.
     * @return un matricule du type "MAT{YY}{NNNN}" jamais utilise auparavant.
     */
    public String generateNextStagiaireMatricule() {
        String yearSuffix = currentYearSuffix();
        String prefix = MATRICULE_PREFIX + yearSuffix;

        int nextCounter = stagiaireRepository.findMaxMatriculeByPrefix(prefix)
                .map(this::extractCounter)
                .orElse(0) + 1;

        if (nextCounter > MAX_COUNTER) {
            throw new IllegalStateException(
                    "Capacite maximale (" + MAX_COUNTER + ") de matricules atteinte pour l'annee " + yearSuffix + ".");
        }

        String matricule = String.format("%s%04d", prefix, nextCounter);
        log.debug("Matricule stagiaire genere : {}", matricule);
        return matricule;
    }

    /** Renvoie les 2 derniers chiffres de l'annee courante (ex : 25 pour 2025). */
    private String currentYearSuffix() {
        int year = LocalDate.now().getYear() % 100;
        return String.format("%02d", year);
    }

    /**
     * Extrait la partie numerique d'un matricule "MAT{YY}{NNNN}".
     * En cas de format inattendu (matricules legacy), renvoie 0 pour repartir proprement.
     */
    private int extractCounter(String matricule) {
        if (matricule == null || matricule.length() < MATRICULE_PREFIX.length() + 2 + COUNTER_WIDTH) {
            return 0;
        }
        String suffix = matricule.substring(matricule.length() - COUNTER_WIDTH);
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ex) {
            log.warn("Format de matricule legacy ignore lors du calcul du compteur : {}", matricule);
            return 0;
        }
    }

    /** Variante exposable au cas ou le suffixe d'annee d'un matricule existant doit etre lu. */
    public Optional<String> peekLastMatriculeForCurrentYear() {
        return stagiaireRepository.findMaxMatriculeByPrefix(MATRICULE_PREFIX + currentYearSuffix());
    }
}
