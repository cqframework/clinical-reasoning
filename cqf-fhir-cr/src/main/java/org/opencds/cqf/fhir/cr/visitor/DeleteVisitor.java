package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

public class DeleteVisitor extends BaseKnowledgeArtifactVisitor {

    public DeleteVisitor(Repository repository) {
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

        for (var res : resourcesToUpdate) {
            var entry = PackageHelper.deleteEntry(res);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }
}
