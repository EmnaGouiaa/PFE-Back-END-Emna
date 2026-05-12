package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountRequest;
import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountResponse;
import fsegs.pfebackendemnagouuiaa.dto.CreateRepresentantEntrepriseRequest;
import fsegs.pfebackendemnagouuiaa.dto.EntrepriseDto;
import fsegs.pfebackendemnagouuiaa.dto.RepresentantEntrepriseResponse;

import java.util.List;

public interface AdminCompanyAccountService {
    List<AdminCompanyAccountResponse> getAll();
    AdminCompanyAccountResponse create(AdminCompanyAccountRequest request);
    AdminCompanyAccountResponse update(Long entrepriseId, AdminCompanyAccountRequest request);
    List<EntrepriseDto> getAllEntreprisesForAdmin();
    RepresentantEntrepriseResponse createRepresentantEntreprise(CreateRepresentantEntrepriseRequest request);
}
