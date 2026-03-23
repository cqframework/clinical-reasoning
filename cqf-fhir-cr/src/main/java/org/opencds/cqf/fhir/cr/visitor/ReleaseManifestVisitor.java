package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor that implements the $release-manifest operation for manifest Libraries (asset-collection).
 * <p>
 * Unlike the standard {@link ReleaseVisitor} which discovers dependencies by recursively walking
 * the component tree, this visitor operates on manifest Libraries that already have pre-computed
 * depends-on entries (typically produced by $data-requirements followed by $infer-manifest-parameters).
 * It resolves unversioned dependency references using the terminology endpoint and updates the
 * manifest metadata for release.
 * <p>
 * This separation exists because the standard $release operation intentionally discards and
 * re-discovers depends-on entries through component traversal, which requires the referenced
 * resources to be available in the local repository. Manifests may reference resources that
 * only exist on an external terminology server.
 */
public class ReleaseManifestVisitor extends ReleaseVisitor {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseManifestVisitor.class);

    public ReleaseManifestVisitor(IRepository repository) {
        super(repository);
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter rootAdapter, IBaseParameters operationParameters) {
        artifactBeingReleasedAdapter = rootAdapter;

        // Validate that the artifact is a Library
        if (!(rootAdapter instanceof ILibraryAdapter)) {
            throw new UnprocessableEntityException("$release-manifest requires a Library resource, found: "
                    + rootAdapter.get().fhirType());
        }

        // Parameter extraction
        final var rootLibrary = rootAdapter.get();
        final var current = new Date();
        final boolean latestFromTxServer = VisitorHelper.getBooleanParameter("latestFromTxServer", operationParameters)
                .orElse(false);
        final Optional<IEndpointAdapter> terminologyEndpoint = VisitorHelper.getResourceParameter(
                        "terminologyEndpoint", operationParameters)
                .map(r -> (IEndpointAdapter) createAdapterForResource(r));
        if (latestFromTxServer && terminologyEndpoint.isEmpty()) {
            throw new UnprocessableEntityException("latestFromTxServer = true but no terminologyEndpoint is available");
        }
        final var version = VisitorHelper.getStringParameter("version", operationParameters)
                .orElseThrow(() -> new UnprocessableEntityException("Version must be present"));
        final var versionBehavior = VisitorHelper.getStringParameter("versionBehavior", operationParameters);
        final var releaseLabel = VisitorHelper.getStringParameter("releaseLabel", operationParameters)
                .orElse("");

        // Validate version and preconditions
        checkReleaseVersion(version, versionBehavior);
        checkReleasePreconditions(rootAdapter, rootAdapter.getApprovalDate());
        updateReleaseLabel(rootLibrary, releaseLabel);

        // Determine release version
        final var existingVersion =
                rootAdapter.hasVersion() ? rootAdapter.getVersion().replace("-draft", "") : null;
        final var releaseVersion = getReleaseVersion(version, versionBehavior, existingVersion, fhirVersion())
                .orElseThrow(
                        () -> new UnprocessableEntityException("Could not resolve a version for the root artifact."));

        // Update metadata on the manifest
        updateMetadata(rootAdapter, releaseVersion, rootAdapter.getEffectivePeriod(), current);

        // Extract expansion parameters for version resolution
        var inputExpansionParams = rootAdapter.getExpansionParameters().orElse(null);
        captureInputExpansionParams(inputExpansionParams, rootAdapter);

        // Resolve existing depends-on entries in place
        var dependencies = new ArrayList<IDependencyInfo>(rootAdapter.getDependencies());
        resolveExistingDependencies(
                dependencies, inputExpansionParams, latestFromTxServer, terminologyEndpoint.orElse(null));

        // Clean up expansion parameters: remove unversioned entries that now have versioned counterparts
        rootAdapter.getExpansionParameters().ifPresent(this::removeSupersededExpansionParams);

        // Build transaction bundle with the updated manifest
        var transactionBundle = BundleHelper.newBundle(fhirVersion(), null, "transaction");
        var entry = PackageHelper.createEntry(rootLibrary, true);
        BundleHelper.addEntry(transactionBundle, entry);

        return repository.transaction(transactionBundle);
    }

    private void resolveExistingDependencies(
            List<IDependencyInfo> dependencies,
            IBaseParameters inputExpansionParameters,
            boolean latestFromTxServer,
            IEndpointAdapter endpoint) {
        for (var dependency : dependencies) {
            try {
                var maybeAdapter =
                        tryResolveDependency(dependency, inputExpansionParameters, latestFromTxServer, endpoint);
                maybeAdapter.ifPresent(adapter -> dependency.setReference(adapter.getCanonical()));
            } catch (Exception e) {
                logger.warn(
                        "Unable to resolve dependency '{}', keeping original reference: {}",
                        dependency.getReference(),
                        e.getMessage());
            }
        }
    }

    /**
     * Removes unversioned expansion parameter entries that have been superseded by versioned entries.
     * After dependency resolution, the expansion parameters may contain both an unversioned entry
     * (e.g., system-version: http://loinc.org) and a versioned entry (e.g., system-version:
     * http://loinc.org|2.81). This method removes the unversioned duplicates.
     */
    private void removeSupersededExpansionParams(IBaseParameters expansionParams) {
        if (expansionParams instanceof org.hl7.fhir.r4.model.Parameters r4Params) {
            removeSupersededR4Params(r4Params);
        } else if (expansionParams instanceof org.hl7.fhir.dstu3.model.Parameters dstu3Params) {
            removeSupersededDstu3Params(dstu3Params);
        } else if (expansionParams instanceof org.hl7.fhir.r5.model.Parameters r5Params) {
            removeSupersededR5Params(r5Params);
        }
    }

    private void removeSupersededR4Params(org.hl7.fhir.r4.model.Parameters params) {
        var allParams = params.getParameter();
        // Collect versioned base URLs by parameter name
        var versionedUrls = new java.util.HashSet<String>();
        for (var param : allParams) {
            var value = param.hasValue() ? param.getValue().primitiveValue() : null;
            if (value != null && Canonicals.getVersion(value) != null) {
                versionedUrls.add(param.getName() + "|" + Canonicals.getUrl(value));
            }
        }
        // Remove unversioned entries that have a versioned counterpart
        allParams.removeIf(param -> {
            var value = param.hasValue() ? param.getValue().primitiveValue() : null;
            return value != null
                    && Canonicals.getVersion(value) == null
                    && versionedUrls.contains(param.getName() + "|" + value);
        });
    }

    private void removeSupersededDstu3Params(org.hl7.fhir.dstu3.model.Parameters params) {
        var allParams = params.getParameter();
        var versionedUrls = new java.util.HashSet<String>();
        for (var param : allParams) {
            var value = param.hasValue() ? param.getValue().primitiveValue() : null;
            if (value != null && Canonicals.getVersion(value) != null) {
                versionedUrls.add(param.getName() + "|" + Canonicals.getUrl(value));
            }
        }
        allParams.removeIf(param -> {
            var value = param.hasValue() ? param.getValue().primitiveValue() : null;
            return value != null
                    && Canonicals.getVersion(value) == null
                    && versionedUrls.contains(param.getName() + "|" + value);
        });
    }

    private void removeSupersededR5Params(org.hl7.fhir.r5.model.Parameters params) {
        var allParams = params.getParameter();
        var versionedUrls = new java.util.HashSet<String>();
        for (var param : allParams) {
            var value = param.hasValue() ? param.getValue().primitiveValue() : null;
            if (value != null && Canonicals.getVersion(value) != null) {
                versionedUrls.add(param.getName() + "|" + Canonicals.getUrl(value));
            }
        }
        allParams.removeIf(param -> {
            var value = param.hasValue() ? param.getValue().primitiveValue() : null;
            return value != null
                    && Canonicals.getVersion(value) == null
                    && versionedUrls.contains(param.getName() + "|" + value);
        });
    }

    private void captureInputExpansionParams(
            IBaseParameters inputExpansionParams, IKnowledgeArtifactAdapter rootAdapter) {
        if (this.fhirVersion().equals(ca.uhn.fhir.context.FhirVersionEnum.DSTU3)) {
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.captureInputExpansionParams(
                    inputExpansionParams, rootAdapter);
        } else if (this.fhirVersion().equals(ca.uhn.fhir.context.FhirVersionEnum.R4)) {
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.captureInputExpansionParams(
                    inputExpansionParams, rootAdapter);
        } else if (this.fhirVersion().equals(ca.uhn.fhir.context.FhirVersionEnum.R5)) {
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.captureInputExpansionParams(
                    inputExpansionParams, rootAdapter);
        }
    }
}
