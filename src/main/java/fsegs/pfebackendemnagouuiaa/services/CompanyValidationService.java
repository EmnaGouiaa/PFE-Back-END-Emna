package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.dto.CompanyValidationItemDto;

import java.util.List;

public interface CompanyValidationService {
    List<CompanyValidationItemDto> getValidationItemsForAuthenticatedCompany();

    CompanyValidationItemDto approve(String itemType, Long itemId);

    CompanyValidationItemDto reject(String itemType, Long itemId, String commentaire);
}
