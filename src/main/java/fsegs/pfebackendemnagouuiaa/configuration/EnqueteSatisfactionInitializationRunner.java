package fsegs.pfebackendemnagouuiaa.configuration;

import fsegs.pfebackendemnagouuiaa.repository.StageRepository;
import fsegs.pfebackendemnagouuiaa.services.EnqueteSatisfactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
public class EnqueteSatisfactionInitializationRunner implements CommandLineRunner {

    private final StageRepository stageRepository;
    private final EnqueteSatisfactionService enqueteSatisfactionService;

    @Override
    public void run(String... args) {
        stageRepository.findAll().forEach(enqueteSatisfactionService::creerEnquetesPourStageSiNecessaire);
    }
}
