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

public class WithdrawVisitor extends BaseKnowledgeArtifactVisitor {

    public WithdrawVisitor(Repository repository) {
        super(repository);
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter rootAdapter, IBaseParameters operationParameters) {
        if (!rootAdapter.getStatus().equals("draft")) {
            throw new PreconditionFailedException("Cannot withdraw an artifact that is not in draft status");
        }
        var fhirVersion = rootAdapter.get().getStructureFhirVersionEnum();
        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");

        var resToUpdate = new ArrayList<IDomainResource>();
        resToUpdate.add(rootAdapter.get());

        findArtifactCommentsToUpdate(rootAdapter.get(), fhirVersion.getFhirVersionString(), repository)
                .forEach(artifact -> {
                    var resource = BundleHelper.getEntryResource(fhirVersion, artifact);
                    var entry = PackageHelper.deleteEntry(resource);
                    BundleHelper.addEntry(transactionBundle, entry);
                });

        var resourcesToUpdate = getComponents(rootAdapter, repository, resToUpdate);

        for (var artifact : resourcesToUpdate) {
            var entry = PackageHelper.deleteEntry(artifact);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }
}
