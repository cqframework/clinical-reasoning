package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;

public class DeleteVisitor extends BaseKnowledgeArtifactVisitor {

    public DeleteVisitor(IRepository repository) {
        super(repository);
    }

    public static final String RETIRED_STATUS = "retired";

    @Override
    public IBase visit(IKnowledgeArtifactAdapter rootAdapter, IBaseParameters operationParams) {
        if (!rootAdapter.getStatus().equals(RETIRED_STATUS)) {
            throw new PreconditionFailedException("Cannot delete an artifact that is not in retired status");
        }

        var fhirVersion = rootAdapter.get().getStructureFhirVersionEnum();
        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");

        var resToUpdate = new ArrayList<IDomainResource>();
        resToUpdate.add(rootAdapter.get());

        var resourcesToUpdate = getComponents(rootAdapter, repository, resToUpdate);

        var resourceReference = rootAdapter.get().getIdElement().getResourceType() + "/"
                + rootAdapter.get().getIdElement().getIdPart();
        var searchParams = Searches.builder()
                .withReferenceParam("artifact", resourceReference)
                .build();
        var searchResult = repository.search(Bundle.class, Basic.class, searchParams);
        var basicResources = BundleHelper.getEntryResources(searchResult);
        for (var basic : basicResources) {
            resourcesToUpdate.add((IDomainResource) basic);
        }

        for (var res : resourcesToUpdate) {
            var entry = PackageHelper.deleteEntry(res);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }
}
