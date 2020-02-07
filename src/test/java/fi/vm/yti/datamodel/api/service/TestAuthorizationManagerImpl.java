package fi.vm.yti.datamodel.api.service;

import java.util.Collection;
import java.util.UUID;

import org.apache.jena.iri.IRI;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fi.vm.yti.datamodel.api.model.AbstractClass;
import fi.vm.yti.datamodel.api.model.AbstractModel;
import fi.vm.yti.datamodel.api.model.AbstractPredicate;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.security.AuthenticatedUserProvider;

@Component
@Profile("test")
public class TestAuthorizationManagerImpl implements AuthorizationManager {

    private final AuthenticatedUserProvider userProvider;

    TestAuthorizationManagerImpl(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @Override
    public boolean hasRightToEdit(final AbstractModel model) {
        return true;
    }

    @Override
    public boolean hasRightToEdit(final AbstractClass model) {
        return true;
    }

    @Override
    public boolean hasRightToEdit(final AbstractPredicate model) {
        return true;
    }

    @Override
    public boolean hasRightToCreateNewVersion(final AbstractModel model) {
        return true;
    }

    @Override
    public boolean hasRightToAnyOrganization(final Collection<UUID> organizations) {
        return true;
    }

    @Override
    public boolean isAdminOfAnyOrganization(final Collection<UUID> organizations) {
        return true;
    }

    @Override
    public boolean hasRightToRemoveClassReference(final IRI modelIRI,
                                                  final IRI classIRI) {
        return true;
    }

    @Override
    public boolean hasRightToAddClassReference(final IRI modelIRI,
                                               final String classId) {
        return true;
    }

    @Override
    public boolean hasRightToAddPredicateReference(final IRI modelIRI,
                                                   final String predicateId) {
        return true;
    }

    @Override
    public boolean hasRightToRemovePredicateReference(final IRI modelIRI,
                                                      final IRI predicateIRI) {
        return true;
    }

    @Override
    public boolean canReplicate() {
        return true;
    }

    @Override
    public boolean hasRightToDoMigration() {
        return true;
    }

    @Override
    public boolean hasRightToRunSparqlQuery() {
        return true;
    }

    @Override
    public boolean hasRightToSuggestConcept() {
        return true;
    }

    @Override
    public boolean hasRightToDropDatabase() {
        return true;
    }
}
