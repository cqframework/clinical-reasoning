package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.findUnsupportedCapability;
import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.processCanonicals;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.util.FhirTerser;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.client.terminology.FederatedTerminologyProviderRouter;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyProviderRouter;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageVisitor extends BaseKnowledgeArtifactVisitor {
    private static final Logger myLogger = LoggerFactory.getLogger(PackageVisitor.class);
    private static final String CANONICAL_TYPE = "canonical";
    private static final String CONFORMANCE_TYPE = "conformance";
    private static final String KNOWLEDGE_ARTIFACT_TYPE = "knowledge";
    private static final String TERMINOLOGY_TYPE = "terminology";
    private static final String VALUESET_FHIR_TYPE = "ValueSet";
    private static final String CRMI_INTENDED_USAGE_CONTEXT_URL =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-intendedUsageContext";
    private static final int MAX_ID_LENGTH = 64;
    private static final String CANONICAL_ENCODED_PREFIX = "cv-";
    private static final Pattern FHIR_ID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-.]+$");
    protected final ITerminologyProviderRouter terminologyServerRouter;
    protected final ExpandHelper expandHelper;

    protected Map<String, List<?>> resourceTypes = new HashMap<>();
    private IBaseOperationOutcome messages;
    private final IAdapterFactory adapterFactory;

    public PackageVisitor(IRepository repository) {
        this(repository, (ITerminologyProviderRouter) null, null);
    }

    public PackageVisitor(IRepository repository, ITerminologyProviderRouter terminologyServerRouter) {
        this(repository, terminologyServerRouter, null);
    }

    public PackageVisitor(IRepository repository, TerminologyServerClientSettings terminologyServerClientSettings) {
        super(repository);
        this.terminologyServerRouter =
                new FederatedTerminologyProviderRouter(fhirContext(), terminologyServerClientSettings);
        this.expandHelper = new ExpandHelper(this.repository, terminologyServerRouter);
        this.adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        setupResourceTypes();
    }

    public PackageVisitor(
            IRepository repository,
            TerminologyServerClientSettings terminologyServerClientSettings,
            IValueSetExpansionCache cache) {
        super(repository, cache);
        this.terminologyServerRouter =
                new FederatedTerminologyProviderRouter(fhirContext(), terminologyServerClientSettings);
        this.expandHelper = new ExpandHelper(this.repository, terminologyServerRouter);
        this.adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        setupResourceTypes();
    }

    public PackageVisitor(
            IRepository repository, ITerminologyProviderRouter terminologyServerRouter, IValueSetExpansionCache cache) {
        super(repository, cache);
        if (terminologyServerRouter == null) {
            this.terminologyServerRouter = new FederatedTerminologyProviderRouter(fhirContext());
        } else {
            this.terminologyServerRouter = terminologyServerRouter;
        }
        expandHelper = new ExpandHelper(this.repository, this.terminologyServerRouter);
        this.adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        setupResourceTypes();
    }

    public void setupResourceTypes() {
        switch (fhirVersion()) {
            case DSTU3:
                resourceTypes.put(
                        CANONICAL_TYPE, org.opencds.cqf.fhir.cr.visitor.dstu3.ResourceTypes.canonicalResourceTypes);
                resourceTypes.put(
                        CONFORMANCE_TYPE, org.opencds.cqf.fhir.cr.visitor.dstu3.ResourceTypes.conformanceResourceTypes);
                resourceTypes.put(
                        KNOWLEDGE_ARTIFACT_TYPE,
                        org.opencds.cqf.fhir.cr.visitor.dstu3.ResourceTypes.knowledgeArtifactResourceTypes);
                resourceTypes.put(
                        TERMINOLOGY_TYPE, org.opencds.cqf.fhir.cr.visitor.dstu3.ResourceTypes.terminologyResourceTypes);
                break;
            case R4:
                resourceTypes.put(
                        CANONICAL_TYPE, org.opencds.cqf.fhir.cr.visitor.r4.ResourceTypes.canonicalResourceTypes);
                resourceTypes.put(
                        CONFORMANCE_TYPE, org.opencds.cqf.fhir.cr.visitor.r4.ResourceTypes.conformanceResourceTypes);
                resourceTypes.put(
                        KNOWLEDGE_ARTIFACT_TYPE,
                        org.opencds.cqf.fhir.cr.visitor.r4.ResourceTypes.knowledgeArtifactResourceTypes);
                resourceTypes.put(
                        TERMINOLOGY_TYPE, org.opencds.cqf.fhir.cr.visitor.r4.ResourceTypes.terminologyResourceTypes);
                break;
            case R5:
                resourceTypes.put(
                        CANONICAL_TYPE, org.opencds.cqf.fhir.cr.visitor.r5.ResourceTypes.canonicalResourceTypes);
                resourceTypes.put(
                        CONFORMANCE_TYPE, org.opencds.cqf.fhir.cr.visitor.r5.ResourceTypes.conformanceResourceTypes);
                resourceTypes.put(
                        KNOWLEDGE_ARTIFACT_TYPE,
                        org.opencds.cqf.fhir.cr.visitor.r5.ResourceTypes.knowledgeArtifactResourceTypes);
                resourceTypes.put(
                        TERMINOLOGY_TYPE, org.opencds.cqf.fhir.cr.visitor.r5.ResourceTypes.terminologyResourceTypes);
                break;

            default:
                break;
        }
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters packageParameters) {
        var fhirVersion = adapter.get().getStructureFhirVersionEnum();

        Optional<String> artifactRoute = VisitorHelper.getStringParameter("artifactRoute", packageParameters);
        Optional<String> endpointUri = VisitorHelper.getStringParameter("endpointUri", packageParameters);
        Optional<IEndpointAdapter> endpoint = VisitorHelper.getResourceParameter("endpoint", packageParameters)
                .map(ep -> (IEndpointAdapter) createAdapterForResource(ep));
        Optional<IEndpointAdapter> terminologyEndpoint = VisitorHelper.getResourceParameter(
                        "terminologyEndpoint", packageParameters)
                .map(ep -> (IEndpointAdapter) createAdapterForResource(ep));
        Optional<Boolean> packageOnly = VisitorHelper.getBooleanParameter("packageOnly", packageParameters);
        Optional<Integer> count = VisitorHelper.getIntegerParameter("count", packageParameters);
        Optional<Integer> offset = VisitorHelper.getIntegerParameter("offset", packageParameters);
        Optional<String> bundleType = VisitorHelper.getStringParameter("bundleType", packageParameters);
        List<String> include = VisitorHelper.getStringListParameter("include", packageParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> exclude = VisitorHelper.getStringListParameter("exclude", packageParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> excludePackageId = VisitorHelper.getStringListParameter("excludePackageId", packageParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> capability = VisitorHelper.getStringListParameter("capability", packageParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> artifactVersion = VisitorHelper.getStringListParameter("artifactVersion", packageParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> checkArtifactVersion = VisitorHelper.getStringListParameter(
                        "checkArtifactVersion", packageParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> forceArtifactVersion = VisitorHelper.getStringListParameter(
                        "forceArtifactVersion", packageParameters)
                .orElseGet(() -> new ArrayList<>());
        boolean isPut =
                VisitorHelper.getBooleanParameter("isPut", packageParameters).orElse(false);

        if ((artifactRoute.isPresent()
                        && !StringUtils.isBlank(artifactRoute.get())
                        && !artifactRoute.get().isEmpty())
                || (endpointUri.isPresent()
                        && !StringUtils.isBlank(endpointUri.get())
                        && !endpointUri.get().isEmpty())
                || endpoint.isPresent()) {
            throw new NotImplementedOperationException(
                    "This repository is not implementing custom Content and endpoints at this time");
        }
        if (count.isPresent() && count.get() < 0) {
            throw new InvalidRequestException("'count' must be non-negative");
        }
        if (offset.isPresent() && offset.get() < 0) {
            throw new InvalidRequestException("'offset' must be non-negative");
        }
        bundleType
                .filter(bt -> bt.equals("transaction") || bt.equals("collection"))
                .ifPresent(bt -> {
                    if (count.isPresent() || offset.isPresent()) {
                        throw new InvalidRequestException(
                                "It is invalid to use paging when requesting a bundle of type '%s'".formatted(bt));
                    }
                });

        // In the case of a released (active) root Library we can depend on the relatedArtifacts as a
        // comprehensive manifest
        var versionTuple = new ImmutableTriple<>(artifactVersion, checkArtifactVersion, forceArtifactVersion);
        var packagedBundle = BundleHelper.newBundle(fhirVersion);

        // Normalize the id of the root manifest artifact based on its canonical URL and version.
        // Because this is the manifest artifact, we will surface a warning if we cannot normalize.
        normalizeIdFromCanonical(adapter, true);

        addBundleEntry(packagedBundle, isPut, adapter);
        if (include.size() == 1 && include.stream().anyMatch(includedType -> includedType.equals("artifact"))) {
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, versionTuple);

            // Normalize the id based on canonical before adding the manifest artifact as a separate entry.
            // Because this is the manifest artifact, we will surface a warning if we cannot normalize.
            normalizeIdFromCanonical(adapter, true);

            var entry = PackageHelper.createEntry(adapter.get(), isPut);
            BundleHelper.addEntry(packagedBundle, entry);
        } else if (packageOnly.orElse(false)) {
            // packageOnly=true: Include only the artifact and its owned components (no recursive dependencies)
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, versionTuple);

            adapter.getOwnedRelatedArtifacts().stream().forEach(c -> {
                final var componentReference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(c);
                Optional<IKnowledgeArtifactAdapter> maybeComponent =
                        VisitorHelper.tryGetLatestVersion(componentReference, repository);
                if (maybeComponent.isPresent()) {
                    var componentAdapter = maybeComponent.get();
                    normalizeIdFromCanonical(componentAdapter, false);
                    addBundleEntry(packagedBundle, isPut, componentAdapter);
                }
            });
        } else {
            // Use array wrapper to allow messages to be updated by recursiveGather
            var messagesWrapper = new IBaseOperationOutcome[] {messages};
            var packagedResources = new HashMap<String, IKnowledgeArtifactAdapter>();
            recursiveGather(
                    adapter,
                    packagedResources,
                    capability,
                    include,
                    versionTuple,
                    terminologyEndpoint.orElse(null),
                    terminologyServerRouter,
                    messagesWrapper);
            messages = messagesWrapper[0]; // Capture any messages created during gathering
            packagedResources.values().stream()
                    .filter(r -> !r.getCanonical().equals(adapter.getCanonical()))
                    .filter(r -> !shouldExcludeByPackage(r, excludePackageId))
                    .forEach(r -> {
                        // Normalize id based on canonical before adding to the package bundle.
                        // For non-manifest artifacts, we do not surface a warning if normalization fails.
                        normalizeIdFromCanonical(r, false);
                        addBundleEntry(packagedBundle, isPut, r);
                    });
            var included = findUnsupportedInclude(BundleHelper.getEntry(packagedBundle), include, adapter, exclude);
            BundleHelper.setEntry(packagedBundle, included);
        }
        handleValueSets(packagedBundle, terminologyEndpoint);
        applyManifestUsageContextsToValueSets(adapter, packagedBundle);

        // Only add messages if there are actual issues
        if (messages != null && ca.uhn.fhir.util.OperationOutcomeUtil.hasIssues(fhirContext(), messages)) {
            messages.setId("messages");
            getRootSpecificationLibrary(packagedBundle).addCqfMessagesExtension(messages);
        }
        setCorrectBundleType(bundleType, count, offset, packagedBundle);
        pageBundleBasedOnCountAndOffset(count, offset, packagedBundle);
        return packagedBundle;

        // DependencyInfo --document here that there is a need for figuring out how to determine which package the
        // dependency is in.
        // what is dependency, where did it originate? potentially the package?
    }

    protected void handleValueSets(IBaseBundle packagedBundle, Optional<IEndpointAdapter> terminologyEndpoint) {
        var expansionParams = newParameters(fhirContext());
        var rootSpecificationLibrary = getRootSpecificationLibrary(packagedBundle);
        if (rootSpecificationLibrary != null) {
            var expansionParamsExtension =
                    rootSpecificationLibrary.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS);
            if (expansionParamsExtension != null && expansionParamsExtension.getValue() != null) {
                expansionParams = getExpansionParams(
                        rootSpecificationLibrary,
                        ((IBaseReference) expansionParamsExtension.getValue())
                                .getReferenceElement()
                                .getValueAsString());
            }
        }
        var params = (IParametersAdapter) createAdapterForResource(
                createAdapterForResource(expansionParams).copy());

        var valueSets = BundleHelper.getEntryResources(packagedBundle).stream()
                .filter(r -> r.fhirType().equals(VALUESET_FHIR_TYPE))
                .map(v -> (IValueSetAdapter) createAdapterForResource(v))
                .collect(Collectors.toList());
        var expansionCache = getExpansionCache();
        var expansionParamsHash = expansionCache.map(
                e -> e.getExpansionParametersHash(rootSpecificationLibrary).orElse(null));
        var missingInCache = new ArrayList<>(valueSets);
        var expandedList = new ArrayList<String>();
        if (expansionCache.isPresent()) {
            var startCache = (new Date()).getTime();
            valueSets.forEach(v -> {
                var cachedExpansion = expansionCache
                        .get()
                        .getExpansionForCanonical(v.getCanonical(), expansionParamsHash.orElse(null));
                if (cachedExpansion != null) {
                    v.setExpansion(cachedExpansion.getExpansion());
                    addExpansionWarningsToOperationOutcome(v);
                    expandedList.add(v.getUrl());
                    missingInCache.remove(v);
                }
            });
            var elapsed = String.valueOf(((new Date()).getTime() - startCache) / 1000);
            myLogger.info("retrieved {} cached ValueSet Expansions in: {}s", expandedList.size(), elapsed);
        }
        missingInCache.forEach(valueSet -> {
            var url = valueSet.getUrl();
            var expansionStartTime = new Date().getTime();
            params.setParameter(params.getParameter().stream()
                    .filter(p -> !List.of(
                                    ITerminologyServerClient.urlParamName, ITerminologyServerClient.versionParamName)
                            .contains(p.getName()))
                    .map(IParametersParameterComponentAdapter::get)
                    .toList());
            try {
                expandHelper.expandValueSet(valueSet, params, terminologyEndpoint, valueSets, expandedList, new Date());
                addExpansionWarningsToOperationOutcome(valueSet);
                var elapsed = String.valueOf(((new Date()).getTime() - expansionStartTime) / 1000);
                myLogger.info("Expanded {} in {}s", url, elapsed);
                if (expansionCache.isPresent()) {
                    expansionCache.get().addToCache(valueSet, expansionParamsHash.orElse(null));
                }
            } catch (Exception e) {
                myLogger.warn("Failed to expand {}. Reporting in outcome manifest", url);
                addMessageIssue("warning", e.getMessage());
            }
        });
    }

    // Helper to surface expansion parameter warnings as OperationOutcome issues
    private void addExpansionWarningsToOperationOutcome(IValueSetAdapter valueSetAdapter) {
        if (valueSetAdapter == null || valueSetAdapter.getExpansion() == null) {
            return;
        }

        var expansion = valueSetAdapter.getExpansion();
        try {
            FhirTerser terser = new FhirTerser(fhirContext());
            var parameters = terser.getValues(expansion, "parameter");
            if (parameters == null || parameters.isEmpty()) {
                return;
            }

            for (var parameter : parameters) {
                String name = terser.getSinglePrimitiveValueOrNull(parameter, "name");
                if (!"warning".equals(name)) {
                    continue;
                }

                String warning = terser.getSinglePrimitiveValueOrNull(parameter, "value");
                if (StringUtils.isBlank(warning)) {
                    continue;
                }

                addMessageIssue("warning", warning);
            }
        } catch (Exception e) {
            myLogger.debug(
                    "Unable to read expansion warning parameters for ValueSet {}: {}",
                    valueSetAdapter.getUrl(),
                    e.getMessage());
        }
    }

    private void addMessageIssue(String severity, String details) {
        if (StringUtils.isBlank(details)) {
            return;
        }

        if (messages == null) {
            messages =
                    OperationOutcomeUtil.createOperationOutcome(severity, details, "processing", fhirContext(), null);
        } else {
            OperationOutcomeUtil.addIssue(fhirContext(), messages, severity, details, null, "processing");
        }
    }

    protected void applyManifestUsageContextsToValueSets(IKnowledgeArtifactAdapter manifest, IBaseBundle bundle) {
        // Build list of ValueSet adapters from bundle
        List<IValueSetAdapter> valueSetResources = BundleHelper.getEntryResources(bundle).stream()
                .filter(r -> r.fhirType().equals(VALUESET_FHIR_TYPE))
                .map(adapterFactory::createValueSet)
                .toList();

        // Filter manifest dependencies to ValueSets only
        List<IDependencyInfo> dependencies = manifest.getDependencies().stream()
                .filter(d -> Objects.equals(Canonicals.getResourceType(d.getReference()), VALUESET_FHIR_TYPE))
                .toList();

        for (IValueSetAdapter valueSetAdapter : valueSetResources) {
            // Build canonical string for matching (url + optional version)
            String canonical = valueSetAdapter.getUrl();
            if (valueSetAdapter.hasVersion()) {
                canonical += "|" + valueSetAdapter.getVersion();
            }

            // Find dependencies that reference this ValueSet
            String finalCanonical = canonical;
            dependencies.stream()
                    .filter(dep -> finalCanonical.equals(dep.getReference()))
                    .forEach(dep ->
                            // Look for crmi-intendedUsageContext extensions
                            dep.getExtension().stream()
                                    .filter(ext -> CRMI_INTENDED_USAGE_CONTEXT_URL.equals(ext.getUrl()))
                                    .forEach(ext -> {
                                        var proposedUsageContextAdapter =
                                                adapterFactory.createUsageContext(ext.getValue());

                                        boolean alreadyExists = false;
                                        for (var uc : valueSetAdapter.getUseContext()) {
                                            var uc1 = adapterFactory.createUsageContext(uc);
                                            if (uc1.equalsDeep(proposedUsageContextAdapter)) {
                                                alreadyExists = true;
                                            }
                                        }

                                        if (!alreadyExists) {
                                            valueSetAdapter.addUseContext(proposedUsageContextAdapter);
                                        }
                                    }));
        }
    }

    public static void setCorrectBundleType(
            Optional<String> requestedBundleType,
            Optional<Integer> count,
            Optional<Integer> offset,
            IBaseBundle bundle) {
        // if paging is used, the bundle type SHALL be searchset, and the resulting bundles SHALL
        // conform to the paging guidance here: https://hl7.org/fhir/R4/http.html#paging.

        var pagingRequested = count.isPresent() || offset.isPresent();

        // If paging is requested, set the bundle type to 'searchset'.
        // Otherwise, use the type requested by the caller ('transaction' or 'collection').
        // If the caller did not request a type and paging is not enabled, default to 'transaction'.
        String bundleType = pagingRequested ? "searchset" : requestedBundleType.orElse("transaction");
        BundleHelper.setBundleType(bundle, bundleType);

        // set total only when paging
        if (pagingRequested) {
            BundleHelper.setBundleTotal(bundle, BundleHelper.getEntry(bundle).size());
        }

        // remove entry.request when paging or when requested bundle type is "collection"
        boolean removeRequests =
                pagingRequested || requestedBundleType.map("collection"::equals).orElse(false);
        if (removeRequests) {
            var cleanedEntries = BundleHelper.getEntry(bundle).stream()
                    .map(entry -> {
                        BundleHelper.setEntryRequest(bundle.getStructureFhirVersionEnum(), entry, null);
                        return entry;
                    })
                    .collect(Collectors.toList());
            BundleHelper.setEntry(bundle, cleanedEntries);
        }
    }

    /**
     * $package allows for a bundle to be paged
     * @param count the maximum number of resources to be returned
     * @param offset the number of resources to skip beginning from the start of the bundle (starts from 1)
     * @param bundle the bundle to page
     */
    public static void pageBundleBasedOnCountAndOffset(
            Optional<Integer> count, Optional<Integer> offset, IBaseBundle bundle) {
        if (offset.isPresent()) {
            var entries = BundleHelper.getEntry(bundle);
            Integer bundleSize = entries.size();
            if (offset.get() < bundleSize) {
                BundleHelper.setEntry(bundle, entries.subList(offset.get(), bundleSize));
            } else {
                BundleHelper.setEntry(bundle, Arrays.asList());
            }
        }
        if (count.isPresent()) {
            // repeat these two from earlier because we might modify / replace the entries list at any time
            var entries = BundleHelper.getEntry(bundle);
            Integer bundleSize = entries.size();
            if (count.get() < bundleSize) {
                BundleHelper.setEntry(bundle, entries.subList(0, count.get()));
            } else {
                // there are not enough entries in the bundle to page, so we return all of them no change
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends IBaseBackboneElement> List<T> findUnsupportedInclude(
            List<T> entries, List<String> include, IKnowledgeArtifactAdapter adapter, List<String> exclude) {

        // CRMI: if include is empty or 'all', return as-is (manifest will already be first)
        if (include == null || include.isEmpty() || include.stream().anyMatch("all"::equals)) {
            // Even if include is empty, still apply exclude filtering
            return applyRoleBasedFiltering(entries, include, exclude, adapter);
        }

        // If include contains ONLY role filters, skip resource type filtering
        if (hasOnlyRoleFilters(include)) {
            return applyRoleBasedFiltering(entries, include, exclude, adapter);
        }

        // 1) Identify the outcome-manifest Library (the "root" Library that corresponds to adapter)
        //    We'll include it unconditionally and keep it first.
        T manifestEntry = null;
        List<T> remainder = new ArrayList<>(entries.size());
        for (T e : entries) {
            var res = BundleHelper.getEntryResource(fhirVersion(), e);
            if (manifestEntry == null && isSameCanonical(res, adapter)) {
                manifestEntry = e;
            } else {
                remainder.add(e);
            }
        }

        // 2) Filter the remainder using existing rules
        List<T> filteredRemainder = new ArrayList<>();
        remainder.forEach(entry -> {
            if (isValidResourceType(include, entry)
                    || isExtensionOrProfile(include, adapter, entry)
                    || isIncludedFhirType(include, entry)) {
                filteredRemainder.add(entry);
            }
            // tests
            if (include.stream().anyMatch("tests"::equals) && isTestCaseEntry(entry)) {
                filteredRemainder.add(entry);
            }
            // examples (placeholder logic retained)
            if (include.stream().anyMatch("examples"::equals) && isExampleEntry(entry)) {
                filteredRemainder.add(entry);
            }
        });

        // 3) Build result with manifest first, then distinct filtered remainder
        List<T> result = new ArrayList<>(entries.size());
        if (manifestEntry != null) {
            result.add(manifestEntry);
        }
        result.addAll(getDistinctFilteredEntries(filteredRemainder));

        // 4) Apply role-based filtering
        return applyRoleBasedFiltering(result, include, exclude, adapter);
    }

    // Helper: compare by canonical URL|version to detect the root manifest library for this package
    private boolean isSameCanonical(IBaseResource res, IKnowledgeArtifactAdapter rootAdapter) {
        if (!"Library".equals(res.fhirType())) return false;
        var af = IAdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum());
        var lib = af.createLibrary((IDomainResource) res);
        // equal when both URL and Version match; tolerate null versions if equal by string
        return lib.getUrl().equals(rootAdapter.getUrl()) && lib.getVersion().equals(rootAdapter.getVersion());
    }

    /**
     * Applies role-based filtering using crmi-dependencyRole extensions from the manifest.
     * Implements the CRMI dependency role filtering model.
     *
     * @param entries the bundle entries to filter
     * @param include list of roles to include (e.g., "key", "test")
     * @param exclude list of roles to exclude (e.g., "example", "test")
     * @param manifestAdapter the root manifest adapter containing relatedArtifacts with role annotations
     * @return filtered list of entries
     */
    @SuppressWarnings("unchecked")
    private <T extends IBaseBackboneElement> List<T> applyRoleBasedFiltering(
            List<T> entries, List<String> include, List<String> exclude, IKnowledgeArtifactAdapter manifestAdapter) {

        // If no role-based filtering is requested, return as-is
        if ((include == null || include.isEmpty() || !hasRoleFilters(include))
                && (exclude == null || exclude.isEmpty())) {
            return entries;
        }

        List<T> result = new ArrayList<>();

        for (T entry : entries) {
            var resource = BundleHelper.getEntryResource(fhirVersion(), entry);

            // Always include the manifest itself
            if (isSameCanonical(resource, manifestAdapter)) {
                result.add(entry);
                continue;
            }

            // Get roles for this dependency from the manifest's relatedArtifacts
            List<String> roles = getDependencyRoles(resource, manifestAdapter);

            // Apply filtering logic
            if (shouldIncludeByRole(roles, include, exclude)) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Checks if the include list contains any role filters (key, default, example, test).
     */
    private boolean hasRoleFilters(List<String> include) {
        if (include == null) {
            return false;
        }
        return include.stream()
                .anyMatch(i -> i.equals("key") || i.equals("default") || i.equals("example") || i.equals("test"));
    }

    /**
     * Checks if the include list contains ONLY role filters (key, default, example, test).
     */
    private boolean hasOnlyRoleFilters(List<String> include) {
        if (include == null || include.isEmpty()) {
            return false;
        }
        return include.stream()
                .allMatch(i -> i.equals("key") || i.equals("default") || i.equals("example") || i.equals("test"));
    }

    /**
     * Retrieves the dependency roles for a resource from the manifest's relatedArtifacts.
     *
     * @param resource the dependency resource
     * @param manifestAdapter the manifest containing relatedArtifacts
     * @return list of role codes (e.g., ["key", "default"])
     */
    @SuppressWarnings("unchecked")
    private List<String> getDependencyRoles(IBaseResource resource, IKnowledgeArtifactAdapter manifestAdapter) {
        List<String> roles = new ArrayList<>();

        try {
            // Get the canonical URL of the resource
            var af = IAdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum());
            var resourceAdapter = af.createKnowledgeArtifactAdapter((IDomainResource) resource);
            String resourceCanonical = resourceAdapter.getUrl();

            // Get relatedArtifacts directly from the manifest resource
            var manifestResource = manifestAdapter.get();
            List<?> relatedArtifacts = null;

            // Extract relatedArtifact elements based on FHIR version
            if (manifestResource instanceof org.hl7.fhir.r4.model.Library) {
                relatedArtifacts = ((org.hl7.fhir.r4.model.Library) manifestResource).getRelatedArtifact();
            } else if (manifestResource instanceof org.hl7.fhir.r5.model.Library) {
                relatedArtifacts = ((org.hl7.fhir.r5.model.Library) manifestResource).getRelatedArtifact();
            } else if (manifestResource instanceof org.hl7.fhir.dstu3.model.Library) {
                relatedArtifacts = ((org.hl7.fhir.dstu3.model.Library) manifestResource).getRelatedArtifact();
            }

            if (relatedArtifacts != null) {
                for (Object ra : relatedArtifacts) {
                    String reference = null;
                    List<?> extensions = null;

                    // Get reference and extensions based on type
                    if (ra instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
                        var r4Ra = (org.hl7.fhir.r4.model.RelatedArtifact) ra;
                        reference = r4Ra.getResource();
                        extensions = r4Ra.getExtension();
                    } else if (ra instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
                        var r5Ra = (org.hl7.fhir.r5.model.RelatedArtifact) ra;
                        reference = r5Ra.getResourceElement() != null
                                ? r5Ra.getResourceElement().getValue()
                                : null;
                        extensions = r5Ra.getExtension();
                    } else if (ra instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
                        var dstu3Ra = (org.hl7.fhir.dstu3.model.RelatedArtifact) ra;
                        reference =
                                dstu3Ra.hasResource() ? dstu3Ra.getResource().getReference() : null;
                        extensions = dstu3Ra.getExtension();
                    }

                    if (reference != null && canonicalMatches(reference, resourceCanonical)) {
                        // Extract crmi-dependencyRole extensions
                        if (extensions != null) {
                            for (Object ext : extensions) {
                                String url = null;
                                Object value = null;

                                if (ext instanceof org.hl7.fhir.r4.model.Extension) {
                                    url = ((org.hl7.fhir.r4.model.Extension) ext).getUrl();
                                    value = ((org.hl7.fhir.r4.model.Extension) ext).getValue();
                                } else if (ext instanceof org.hl7.fhir.r5.model.Extension) {
                                    url = ((org.hl7.fhir.r5.model.Extension) ext).getUrl();
                                    value = ((org.hl7.fhir.r5.model.Extension) ext).getValue();
                                } else if (ext instanceof org.hl7.fhir.dstu3.model.Extension) {
                                    url = ((org.hl7.fhir.dstu3.model.Extension) ext).getUrl();
                                    value = ((org.hl7.fhir.dstu3.model.Extension) ext).getValue();
                                }

                                if (Constants.CRMI_DEPENDENCY_ROLE.equals(url) && value instanceof IPrimitiveType<?>) {
                                    String role = ((IPrimitiveType<?>) value).getValueAsString();
                                    if (role != null && !roles.contains(role)) {
                                        roles.add(role);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }

            // If no roles found, default to "default" role
            if (roles.isEmpty()) {
                roles.add("default");
            }
        } catch (Exception e) {
            myLogger.debug("Error getting dependency roles for resource", e);
            // On error, default to "default" role
            if (roles.isEmpty()) {
                roles.add("default");
            }
        }

        return roles;
    }

    /**
     * Checks if two canonical URLs match (ignoring versions).
     */
    private boolean canonicalMatches(String canonical1, String canonical2) {
        if (canonical1 == null || canonical2 == null) {
            return false;
        }

        // Strip version from both
        String url1 = canonical1.contains("|") ? canonical1.substring(0, canonical1.indexOf('|')) : canonical1;
        String url2 = canonical2.contains("|") ? canonical2.substring(0, canonical2.indexOf('|')) : canonical2;

        return url1.equals(url2);
    }

    /**
     * Determines if a dependency should be included based on its roles and the include/exclude filters.
     *
     * @param roles the roles assigned to this dependency
     * @param include list of roles to include (empty = include all)
     * @param exclude list of roles to exclude
     * @return true if the dependency should be included
     */
    private boolean shouldIncludeByRole(List<String> roles, List<String> include, List<String> exclude) {
        // If exclude is specified and this dependency has an excluded role, exclude it
        if (exclude != null && !exclude.isEmpty()) {
            for (String role : roles) {
                if (exclude.contains(role)) {
                    return false;
                }
            }
        }

        // If include is specified with role filters, only include if it has an included role
        if (include != null && !include.isEmpty() && hasRoleFilters(include)) {
            for (String role : roles) {
                if (include.contains(role)) {
                    return true;
                }
            }
            return false;
        }

        // Default: include
        return true;
    }

    // Extract existing test/example checks into helpers (no behavior change)
    @SuppressWarnings("unchecked")
    private <T extends IBaseBackboneElement> boolean isTestCaseEntry(T entry) {
        var af = IAdapterFactory.forFhirVersion(fhirVersion());
        var r = BundleHelper.getEntryResource(fhirVersion(), entry);
        return ("Library".equals(r.fhirType())
                        && af.createCodeableConcept(af.createLibrary(r).getType())
                                .hasCoding("test-case"))
                || (((IDomainResource) r)
                        .getExtension().stream()
                                .anyMatch(ext -> ext.getUrl().contains("isTestCase")
                                        && ((IPrimitiveType<Boolean>) ext.getValue()).getValue()));
    }

    @SuppressWarnings("unchecked")
    private <T extends IBaseBackboneElement> boolean isExampleEntry(T entry) {
        // TODO: This is a placeholder for now - validate functionality once example include is implemented in full
        var r = BundleHelper.getEntryResource(fhirVersion(), entry);
        return ((IDomainResource) r)
                .getExtension().stream()
                        .anyMatch(ext -> ext.getUrl().contains("isExample")
                                && ((IPrimitiveType<Boolean>) ext.getValue()).getValue());
    }

    private <T extends IBaseBackboneElement> boolean isExtensionOrProfile(
            List<String> include, IKnowledgeArtifactAdapter adapter, T entry) {
        return include.stream().anyMatch(type -> type.equals("extensions") || type.equals("profiles"))
                && BundleHelper.getEntryResource(fhirVersion(), entry)
                        .fhirType()
                        .equals("StructureDefinition")
                && adapter.resolvePathString(BundleHelper.getEntryResource(fhirVersion(), entry), "type")
                        .equals("Extension");
    }

    private <T extends IBaseBackboneElement> boolean isIncludedFhirType(List<String> include, T entry) {
        return include.contains(
                BundleHelper.getEntryResource(fhirVersion(), entry).fhirType());
    }

    protected <T extends IBaseBackboneElement> boolean isValidResourceType(List<String> include, T entry) {
        return (include.stream().anyMatch(type -> type.equals(KNOWLEDGE_ARTIFACT_TYPE))
                        && resourceIsOfType(entry, KNOWLEDGE_ARTIFACT_TYPE))
                || (include.stream()
                        .anyMatch(type -> type.equals(CANONICAL_TYPE) && resourceIsOfType(entry, CANONICAL_TYPE)))
                || (include.stream()
                        .anyMatch(type -> type.equals(CONFORMANCE_TYPE) && resourceIsOfType(entry, CONFORMANCE_TYPE)))
                || (include.stream()
                        .anyMatch(type -> type.equals(TERMINOLOGY_TYPE) && resourceIsOfType(entry, TERMINOLOGY_TYPE)));
    }

    protected <T extends IBaseBackboneElement> List<T> getDistinctFilteredEntries(List<T> filteredList) {
        List<T> distinctFilteredEntries = new ArrayList<>();
        // remove duplicates
        for (var entry : filteredList) {
            if (distinctFilteredEntries.stream()
                    .map(e -> (IAdapterFactory.forFhirVersion(fhirVersion())
                            .createKnowledgeArtifactAdapter(
                                    (IDomainResource) BundleHelper.getEntryResource(fhirVersion(), e))))
                    .noneMatch(existingEntry -> {
                        var resource = IAdapterFactory.forFhirVersion(fhirVersion())
                                .createKnowledgeArtifactAdapter(
                                        (IDomainResource) BundleHelper.getEntryResource(fhirVersion(), entry));
                        return existingEntry.getUrl().equals(resource.getUrl())
                                && existingEntry.getVersion().equals(resource.getVersion());
                    })) {
                distinctFilteredEntries.add(entry);
            }
        }
        return distinctFilteredEntries;
    }

    protected <T extends IBaseBackboneElement> boolean resourceIsOfType(T entry, String type) {
        return resourceTypes.get(type).contains(getResourceType(BundleHelper.getEntryResource(fhirVersion(), entry)));
    }

    @SuppressWarnings("rawtypes")
    protected Enum getResourceType(IBaseResource resource) {
        switch (fhirVersion()) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.Resource) resource).getResourceType();
            case R4:
                return ((org.hl7.fhir.r4.model.Resource) resource).getResourceType();
            case R5:
                return ((org.hl7.fhir.r5.model.Resource) resource).getResourceType();
            default:
                return null;
        }
    }

    protected static ILibraryAdapter getRootSpecificationLibrary(IBaseBundle bundle) {
        Optional<ILibraryAdapter> rootSpecLibrary = BundleHelper.getEntryResources(bundle).stream()
                .filter(r -> r.fhirType().equals("Library"))
                .map(r -> IAdapterFactory.forFhirVersion(r.getStructureFhirVersionEnum())
                        .createLibrary(r))
                // .filter(a -> a.getType().hasCoding(Constants.LIBRARY_TYPE, Constants.ASSET_COLLECTION)
                //         && a.getUseContext().stream()
                //                 .allMatch(useContext -> (useContext
                //                                         .getCode()
                //                                         .getSystem()
                //                                         .equals(KnowledgeArtifactAdapter.usPhContextTypeUrl)
                //                                 && useContext
                //                                         .getCode()
                //                                         .getCode()
                //                                         .equals("reporting")
                //                                 && useContext
                //                                         .getValueCodeableConcept()
                //                                         .hasCoding(Constants.US_PH_CONTEXT_URL, "triggering"))
                //                         || (useContext
                //                                         .getCode()
                //                                         .getSystem()
                //                                         .equals(KnowledgeArtifactAdapter.usPhContextTypeUrl)
                //                                 && useContext
                //                                         .getCode()
                //                                         .getCode()
                //                                         .equals("specification-type")
                //                                 && useContext
                //                                         .getValueCodeableConcept()
                //                                         .hasCoding(Constants.US_PH_CONTEXT_URL, "program"))))
                .findFirst();
        return rootSpecLibrary.orElse(null);
    }

    protected static IBaseParameters getExpansionParams(ILibraryAdapter rootSpecificationLibrary, String reference) {
        Optional<? extends IBaseResource> expansionParamResource = rootSpecificationLibrary.getContained().stream()
                .filter(contained ->
                        reference.equals("#" + contained.getIdElement().getValue()))
                .findFirst();
        return (IBaseParameters) expansionParamResource.orElse(null);
    }

    /**
     * Normalize the id of the given knowledge artifact so that it is derived from the canonical URL
     * and version in a deterministic, FHIR-id-safe way whenever possible.
     *
     * <p>The normalization uses the following strategy:
     * <ol>
     *   <li>If the artifact has no canonical URL, the id is left unchanged.</li>
     *   <li>For typical cases, the id is set to {@code &lt;canonicalTail&gt;[-&lt;version&gt;]} when both
     *       the tail and version consist only of allowed FHIR id characters and the result is at most
     *       64 characters.</li>
     *   <li>If the tail-plus-version form is not usable (for example, due to disallowed characters or
     *       excessive length), the full {@code canonical[|version]} string is Base64-encoded and
     *       converted to a FHIR-id-safe alphabet with the {@code cv-} prefix.</li>
     *   <li>If neither a tail-based nor a cv-encoded form can be represented within the FHIR id
     *       constraints, the existing id is retained. For the manifest artifact, a warning is
     *       surfaced in the outcome manifest to indicate that the id could not be normalized
     *       without loss.</li>
     * </ol>
     *
     * @param adapter the knowledge artifact whose id should be normalized; may be {@code null}, in which
     *                case this method is a no-op
     * @param warnIfNotNormalized if {@code true}, emits a warning via the outcome manifest when the id
     *                            cannot be normalized non-lossily
     */
    private void normalizeIdFromCanonical(IKnowledgeArtifactAdapter adapter, boolean warnIfNotNormalized) {
        if (adapter == null) {
            return;
        }

        String url = adapter.getUrl();
        if (StringUtils.isBlank(url)) {
            // No canonical URL available; leave id as-is.
            return;
        }

        String version = adapter.getVersion();
        String identityString = StringUtils.isBlank(version) ? url : url + "|" + version;

        // 1) Try a human-friendly tail[-version] id when both parts are simple and short enough.
        String tail = getCanonicalTail(url);
        if (StringUtils.isNotBlank(tail)
                && isFhirIdSafe(tail)
                && (StringUtils.isBlank(version) || isFhirIdSafe(version))) {

            String candidateId = StringUtils.isBlank(version) ? tail : tail + "-" + version;
            if (isValidFhirId(candidateId)) {
                adapter.setId(Ids.newId(fhirContext(), candidateId));
                return;
            }
        }

        // 2) Encode the full canonical[|version] string using a FHIR-id-safe Base64 variant.
        String encoded = encodeToIdSafeBase64(identityString);
        String encodedId = CANONICAL_ENCODED_PREFIX + encoded;
        if (encodedId.length() <= MAX_ID_LENGTH) {
            adapter.setId(Ids.newId(fhirContext(), encodedId));
            return;
        }

        // 3) At this point, we cannot represent the canonical|version non-lossily within FHIR id
        // constraints. Retain the existing id and optionally surface a warning for the manifest.
        String existingId = adapter.get().getIdElement().getIdPart();
        String message =
                "The id for resource %s (%s) could not be normalized from canonical '%s' without loss; retaining existing id '%s'."
                        .formatted(
                                adapter.get().fhirType(),
                                adapter.getUrl(),
                                identityString,
                                existingId != null ? existingId : "");

        myLogger.warn(message);

        if (warnIfNotNormalized) {
            if (messages == null) {
                messages = OperationOutcomeUtil.createOperationOutcome(
                        "warning", message, "processing", fhirContext(), null);
            } else {
                OperationOutcomeUtil.addIssue(fhirContext(), messages, "warning", message, null, "processing");
            }
        }
    }

    private static String getCanonicalTail(String url) {
        int idx = url.lastIndexOf('/');
        return idx >= 0 && idx + 1 < url.length() ? url.substring(idx + 1) : url;
    }

    /**
     * Determines if an artifact should be excluded based on its package source.
     * Implements the CRMI safety model: if no package source can be determined, do not exclude.
     *
     * @param adapter the artifact adapter
     * @param excludePackageId list of package IDs to exclude
     * @return true if the artifact should be excluded, false otherwise
     */
    private boolean shouldExcludeByPackage(IKnowledgeArtifactAdapter adapter, List<String> excludePackageId) {
        if (excludePackageId == null || excludePackageId.isEmpty()) {
            return false;
        }

        // Try to get package-source extension directly from the resource
        String pkgSrc = null;
        try {
            var resource = adapter.get();
            if (resource instanceof IBaseHasExtensions) {
                var extensions = ((IBaseHasExtensions) resource).getExtension();
                for (var ext : extensions) {
                    if (Constants.PACKAGE_SOURCE.equals(ext.getUrl())) {
                        Object value = ext.getValue();
                        if (value instanceof IPrimitiveType<?>) {
                            pkgSrc = ((IPrimitiveType<?>) value).getValueAsString();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            myLogger.debug("Error getting package-source extension from resource", e);
        }

        // Fallback to PackageSourceResolver if extension not found directly
        if (pkgSrc == null) {
            Optional<String> packageSource = PackageSourceResolver.resolvePackageSource(adapter, repository);
            if (packageSource.isEmpty()) {
                // Safety model: if we can't determine the package, don't exclude
                return false;
            }
            pkgSrc = packageSource.get();
        }

        // Check if this package matches any excluded package ID
        // Package source format is "packageId" or "packageId#version"
        for (String excludePkg : excludePackageId) {
            // Match either exact package ID or package ID with version
            if (pkgSrc.equals(excludePkg) || pkgSrc.startsWith(excludePkg + "#")) {
                return true;
            }
        }

        return false;
    }

    private static boolean isFhirIdSafe(String value) {
        return value != null && FHIR_ID_PATTERN.matcher(value).matches();
    }

    private static boolean isValidFhirId(String value) {
        return value != null
                && value.length() <= MAX_ID_LENGTH
                && FHIR_ID_PATTERN.matcher(value).matches();
    }

    /**
     * Encode the given string into a FHIR-id-safe Base64 representation using only
     * characters allowed by the FHIR id regex ([A-Za-z0-9-.]).
     *
     * <p>This uses standard Base64, then replaces '+' with '-' and '/' with '.', and
     * strips any trailing '=' padding. This mapping is reversible because the Base64
     * alphabet does not include '-' or '.'.
     *
     * @param value the string to encode
     * @return an id-safe encoded string
     */
    private static String encodeToIdSafeBase64(String value) {
        if (value == null) {
            return "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        // Remove padding and replace disallowed characters with allowed equivalents.
        base64 = base64.replace("=", "").replace('+', '-').replace('/', '.');
        return base64;
    }
}
