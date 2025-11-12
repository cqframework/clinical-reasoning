package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.findUnsupportedCapability;
import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.processCanonicals;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
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
    protected final TerminologyServerClient terminologyServerClient;
    protected final ExpandHelper expandHelper;

    protected Map<String, List<?>> resourceTypes = new HashMap<>();
    private IBaseOperationOutcome messages;
    private final IAdapterFactory adapterFactory;

    public PackageVisitor(IRepository repository) {
        this(repository, (TerminologyServerClient) null, null);
    }

    public PackageVisitor(IRepository repository, TerminologyServerClient client) {
        this(repository, client, null);
    }

    public PackageVisitor(IRepository repository, TerminologyServerClientSettings terminologyServerClientSettings) {
        super(repository);
        this.terminologyServerClient = new TerminologyServerClient(fhirContext(), terminologyServerClientSettings);
        this.expandHelper = new ExpandHelper(this.repository, terminologyServerClient);
        this.adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        setupResourceTypes();
    }

    public PackageVisitor(
            IRepository repository,
            TerminologyServerClientSettings terminologyServerClientSettings,
            IValueSetExpansionCache cache) {
        super(repository, cache);
        this.terminologyServerClient = new TerminologyServerClient(fhirContext(), terminologyServerClientSettings);
        this.expandHelper = new ExpandHelper(this.repository, terminologyServerClient);
        this.adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        setupResourceTypes();
    }

    public PackageVisitor(IRepository repository, TerminologyServerClient client, IValueSetExpansionCache cache) {
        super(repository, cache);
        if (client == null) {
            terminologyServerClient = new TerminologyServerClient(fhirContext());
        } else {
            terminologyServerClient = client;
        }
        expandHelper = new ExpandHelper(this.repository, terminologyServerClient);
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
        if (packageOnly.isPresent()) {
            throw new NotImplementedOperationException("This repository is not implementing packageOnly at this time");
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
        addBundleEntry(packagedBundle, isPut, adapter);
        if (include.size() == 1 && include.stream().anyMatch(includedType -> includedType.equals("artifact"))) {
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, versionTuple);
            var entry = PackageHelper.createEntry(adapter.get(), isPut);
            BundleHelper.addEntry(packagedBundle, entry);
        } else {
            var packagedResources = new HashMap<String, IKnowledgeArtifactAdapter>();
            recursiveGather(
                    adapter,
                    packagedResources,
                    capability,
                    include,
                    versionTuple,
                    terminologyEndpoint.orElse(null),
                    terminologyServerClient);
            packagedResources.values().stream()
                    .filter(r -> !r.getCanonical().equals(adapter.getCanonical()))
                    .forEach(r -> addBundleEntry(packagedBundle, isPut, r));
            var included = findUnsupportedInclude(BundleHelper.getEntry(packagedBundle), include, adapter);
            BundleHelper.setEntry(packagedBundle, included);
        }
        handleValueSets(packagedBundle, terminologyEndpoint);
        applyManifestUsageContextsToValueSets(adapter, packagedBundle);

        if (messages != null) {
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
                                    TerminologyServerClient.urlParamName, TerminologyServerClient.versionParamName)
                            .contains(p.getName()))
                    .map(IParametersParameterComponentAdapter::get)
                    .toList());
            try {
                expandHelper.expandValueSet(valueSet, params, terminologyEndpoint, valueSets, expandedList, new Date());
                var elapsed = String.valueOf(((new Date()).getTime() - expansionStartTime) / 1000);
                myLogger.info("Expanded {} in {}s", url, elapsed);
                if (expansionCache.isPresent()) {
                    expansionCache.get().addToCache(valueSet, expansionParamsHash.orElse(null));
                }
            } catch (Exception e) {
                myLogger.warn("Failed to expand {}. Reporting in outcome manifest", url);
                if (messages == null) {
                    messages = OperationOutcomeUtil.createOperationOutcome(
                            "warning", e.getMessage(), "processing", fhirContext(), null);
                } else {
                    OperationOutcomeUtil.addIssue(
                            fhirContext(), messages, "warning", e.getMessage(), null, "processing");
                }
            }
        });
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
            List<T> entries, List<String> include, IKnowledgeArtifactAdapter adapter) {

        // CRMI: if include is empty or 'all', return as-is (manifest will already be first)
        if (include == null || include.isEmpty() || include.stream().anyMatch("all"::equals)) {
            return entries;
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
        return result;
    }

    // Helper: compare by canonical URL|version to detect the root manifest library for this package
    private boolean isSameCanonical(IBaseResource res, IKnowledgeArtifactAdapter rootAdapter) {
        if (!"Library".equals(res.fhirType())) return false;
        var af = IAdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum());
        var lib = af.createLibrary((IDomainResource) res);
        // equal when both URL and Version match; tolerate null versions if equal by string
        return lib.getUrl().equals(rootAdapter.getUrl()) && lib.getVersion().equals(rootAdapter.getVersion());
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
}
