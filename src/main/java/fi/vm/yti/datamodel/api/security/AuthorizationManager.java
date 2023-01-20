package fi.vm.yti.datamodel.api.security;

import java.util.Collection;
import java.util.UUID;

import org.apache.jena.iri.IRI;

import fi.vm.yti.datamodel.api.model.AbstractClass;
import fi.vm.yti.datamodel.api.model.AbstractModel;
import fi.vm.yti.datamodel.api.model.AbstractPredicate;
import org.apache.jena.rdf.model.Model;

public interface AuthorizationManager {

    boolean hasRightToEdit(AbstractModel model);

    boolean hasRightToEdit(AbstractClass model);

    boolean hasRightToEdit(AbstractPredicate model);

    boolean hasRightToCreateNewVersion(AbstractModel model);

    boolean hasRightToAnyOrganization(Collection<UUID> organizations);

    boolean hasRightToModel(String prefix, Model model);

    boolean isAdminOfAnyOrganization(Collection<UUID> organizations);

    boolean hasRightToRemoveClassReference(IRI modelIRI,
                                                  IRI classIRI);

    boolean hasRightToAddClassReference(IRI modelIRI,
                                               String classId);

    boolean hasRightToAddPredicateReference(IRI modelIRI,
                                                   String predicateId);
    boolean hasRightToRemovePredicateReference(IRI modelIRI,
                                                      IRI predicateIRI);
    boolean canReplicate();

    boolean hasRightToDoMigration();

    boolean hasRightToRunSparqlQuery();

    boolean hasRightToSuggestConcept();
    boolean hasRightToDropDatabase();

}
