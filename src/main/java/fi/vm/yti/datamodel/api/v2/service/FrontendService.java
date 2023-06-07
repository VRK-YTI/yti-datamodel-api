package fi.vm.yti.datamodel.api.v2.service;

import static fi.vm.yti.datamodel.api.v2.dto.ModelConstants.*;

import fi.vm.yti.datamodel.api.v2.dto.OrganizationDTO;
import fi.vm.yti.datamodel.api.v2.dto.ServiceCategoryDTO;
import fi.vm.yti.datamodel.api.v2.mapper.OrganizationMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ServiceCategoryMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrontendService {

    private final JenaService jenaService;
    public FrontendService(JenaService jenaService) {
        this.jenaService = jenaService;
    }

    public List<OrganizationDTO> getOrganizations(@NotNull String sortLanguage, boolean includeChildOrganizations) {
        var organizations = jenaService.getOrganizations();
        var dtos = OrganizationMapper.mapToListOrganizationDTO(organizations);

        dtos.sort((a, b) -> {
            var labelA = a.getLabel().getOrDefault(sortLanguage, a.getLabel().get(DEFAULT_LANGUAGE));
            var labelB = b.getLabel().getOrDefault(sortLanguage, b.getLabel().get(DEFAULT_LANGUAGE));
            return labelA.compareTo(labelB);
        });

        return includeChildOrganizations ? dtos : dtos.stream()
                .filter(dto -> dto.getParentOrganization() == null)
                .toList();
    }

    public List<ServiceCategoryDTO> getServiceCategories(@NotNull String sortLanguage) {
        var serviceCategories = jenaService.getServiceCategories();
        var dtos = ServiceCategoryMapper.mapToListServiceCategoryDTO(serviceCategories);

        dtos.sort((a, b) -> {
            var labelA = a.getLabel().getOrDefault(sortLanguage, a.getLabel().get(DEFAULT_LANGUAGE));
            var labelB = b.getLabel().getOrDefault(sortLanguage, b.getLabel().get(DEFAULT_LANGUAGE));
            return labelA.compareTo(labelB);
        });

        return dtos;
    }
}
