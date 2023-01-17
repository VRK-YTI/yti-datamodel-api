package fi.vm.yti.datamodel.api.v2.service;

import static fi.vm.yti.datamodel.api.v2.dto.ModelConstants.*;

import fi.vm.yti.datamodel.api.v2.dto.BaseDTO;
import fi.vm.yti.datamodel.api.v2.dto.OrganizationDTO;
import fi.vm.yti.datamodel.api.v2.dto.ServiceCategoryDTO;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FrontendService {

    private final JenaService jenaService;
    private final ModelMapper modelMapper;

    public FrontendService(JenaService jenaService, ModelMapper modelMapper) {
        this.jenaService = jenaService;
        this.modelMapper = modelMapper;
    }

    public List<OrganizationDTO> getOrganizations(@NotNull String sortLanguage, boolean includeChildOrganizations) {
        var organizations = jenaService.getOrganizations();
        var dtos = modelMapper.mapToListOrganizationDTO(organizations);

        sortByLabel(sortLanguage, dtos);

        return includeChildOrganizations ? dtos : dtos.stream()
                .filter(dto -> dto.getParentOrganization() == null)
                .collect(Collectors.toList());
    }

    public List<ServiceCategoryDTO> getServiceCategories(@NotNull String sortLanguage) {
        var serviceCategories = jenaService.getServiceCategories();
        var dtos = modelMapper.mapToListServiceCategoryDTO(serviceCategories);

        sortByLabel(sortLanguage, dtos);

        return dtos;
    }

    private void sortByLabel(@NotNull String sortLanguage, List<? extends BaseDTO> dtos) {
        dtos.sort((a, b) -> {
            var label_a = a.getLabel().getOrDefault(sortLanguage, a.getLabel().get(DEFAULT_LANGUAGE));
            var label_b = b.getLabel().getOrDefault(sortLanguage, b.getLabel().get(DEFAULT_LANGUAGE));
            return label_a.compareTo(label_b);
        });
    }
}
