package fi.vm.yti.datamodel.api.v2.validator.release;

import fi.vm.yti.datamodel.api.v2.dto.ResourceReferenceDTO;
import org.apache.jena.rdf.model.Model;

import java.util.Set;

public abstract class ReleaseValidator {

    public abstract Set<ResourceReferenceDTO> validate(Model model);

    public abstract String getErrorKey();
}
