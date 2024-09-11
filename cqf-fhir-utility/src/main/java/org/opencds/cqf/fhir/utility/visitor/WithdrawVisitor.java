package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WithdrawVisitor implements KnowledgeArtifactVisitor {
    private Logger log = LoggerFactory.getLogger(WithdrawVisitor.class);

    @Override
    public IBase visit(
            KnowledgeArtifactAdapter rootAdapter, Repository repository, IBaseParameters operationParameters) {
        if (!rootAdapter.getStatus().equals("draft")) {
            throw new PreconditionFailedException("Cannot withdraw an artifact that is not in draft status");
        }
        var resToUpdate = new ArrayList<IDomainResource>();
        resToUpdate.add(rootAdapter.get());

        var resourcesToUpdate = gatherDependsOnChildren(rootAdapter, repository, resToUpdate);

        var fhirVersion = rootAdapter.get().getStructureFhirVersionEnum();

        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");
        for (var artifact : resourcesToUpdate) {
            var entry = PackageHelper.deleteEntry(artifact);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }

    private List<IDomainResource> gatherDependsOnChildren(KnowledgeArtifactAdapter adapter, Repository repository, ArrayList<IDomainResource> resourcesToUpdate) {
        adapter.getRelatedArtifactsOfType("depends-on").stream().forEach(c -> {
            final var preReleaseReference = KnowledgeArtifactAdapter.getRelatedArtifactReference(c);
            Optional<KnowledgeArtifactAdapter> maybeArtifact = VisitorHelper.tryGetLatestVersion(preReleaseReference, repository);
            if (maybeArtifact.isPresent()) {
                if (resourcesToUpdate.stream().filter(rtu -> rtu.getId().equals(maybeArtifact.get().getId().toString())).collect(
                    Collectors.toList()).isEmpty()) {
                    resourcesToUpdate.add(maybeArtifact.get().get());
                    gatherDependsOnChildren(maybeArtifact.get(), repository, resourcesToUpdate);
                }
            }
        });

        return resourcesToUpdate;
    }
}
