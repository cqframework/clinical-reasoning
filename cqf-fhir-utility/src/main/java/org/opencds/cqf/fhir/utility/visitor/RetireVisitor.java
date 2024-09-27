package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import java.util.Date;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;

public class RetireVisitor extends AbstractKnowledgeArtifactVisitor {

    @Override
    public IBase visit(
            KnowledgeArtifactAdapter rootAdapter, Repository repository, IBaseParameters operationParameters) {
        if (!rootAdapter.getStatus().equals("draft")) {
            throw new PreconditionFailedException("Cannot withdraw an artifact that is not in draft status");
        }
        var fhirVersion = rootAdapter.get().getStructureFhirVersionEnum();
        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");

        var resToUpdate = new ArrayList<IDomainResource>();
        resToUpdate.add(rootAdapter.get());

        var resourcesToUpdate = getComponents(rootAdapter, repository, resToUpdate);

        var nowDate = new Date();

        for (var resource : resourcesToUpdate) {
            var artifact = AdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                    .createKnowledgeArtifactAdapter(resource);
            updateMetadata(artifact, nowDate);
            var entry = PackageHelper.createEntry(artifact.get(), true);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }

    private static void updateMetadata(KnowledgeArtifactAdapter artifactAdapter, Date date) {
        artifactAdapter.setDate(date);
        artifactAdapter.setStatus("retired");
    }
}
