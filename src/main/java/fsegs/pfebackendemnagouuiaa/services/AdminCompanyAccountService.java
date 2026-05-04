package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountRequest;
import fsegs.pfebackendemnagouuiaa.dto.AdminCompanyAccountResponse;

import java.util.List;

public interface AdminCompanyAccountService {
    List<AdminCompanyAccountResponse> getAll();
    AdminCompanyAccountResponse create(AdminCompanyAccountRequest request);
    AdminCompanyAccountResponse update(Long entrepriseId, AdminCompanyAccountRequest request);
}
