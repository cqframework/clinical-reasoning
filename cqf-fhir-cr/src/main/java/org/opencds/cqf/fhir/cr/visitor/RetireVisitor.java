package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import java.util.Date;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

public class RetireVisitor extends BaseKnowledgeArtifactVisitor {

    public RetireVisitor(IRepository repository) {
        super(repository);
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter rootAdapter, IBaseParameters operationParameters) {
        if (!rootAdapter.getStatus().equals("active")) {
            throw new PreconditionFailedException("Cannot retire an artifact that is not in active status");
        }
        var fhirVersion = rootAdapter.get().getStructureFhirVersionEnum();
        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");

        var resToUpdate = new ArrayList<IDomainResource>();
        resToUpdate.add(rootAdapter.get());

        var resourcesToUpdate = getComponents(rootAdapter, repository, resToUpdate);

        var nowDate = new Date();

        for (var resource : resourcesToUpdate) {
            var artifact = IAdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                    .createKnowledgeArtifactAdapter(resource);
            updateMetadata(artifact, nowDate);
            var entry = PackageHelper.createEntry(artifact.get(), true);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }

    private static void updateMetadata(IKnowledgeArtifactAdapter artifactAdapter, Date date) {
        artifactAdapter.setDate(date);
        artifactAdapter.setStatus("retired");
    }
}
