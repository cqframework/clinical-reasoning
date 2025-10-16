package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WithdrawVisitor extends BaseKnowledgeArtifactVisitor {

    private Logger logger = LoggerFactory.getLogger(WithdrawVisitor.class);

    public WithdrawVisitor(IRepository repository) {
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

        try {
            findArtifactCommentsToUpdate(rootAdapter.get(), fhirVersion.getFhirVersionString(), repository)
                    .forEach(artifact -> {
                        var resource = BundleHelper.getEntryResource(fhirVersion, artifact);
                        var entry = PackageHelper.deleteEntry(resource);
                        BundleHelper.addEntry(transactionBundle, entry);
                    });
        } catch (Exception e) {
            logger.error("Error encountered attempting to delete ArtifactComments: {}", e.getMessage());
        }

        var resourcesToUpdate = getComponents(rootAdapter, repository, resToUpdate);

        for (var artifact : resourcesToUpdate) {
            var entry = PackageHelper.deleteEntry(artifact);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }
}
