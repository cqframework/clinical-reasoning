package org.opencds.cqf.fhir.cr.visitor;

import static kotlinx.io.CoreKt.buffered;
import static kotlinx.io.JvmCoreKt.asSource;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_30_50;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv30_50.VersionConvertor_30_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Library;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.cr.implementationguide.ImplementationGuidePackageResolver;
import org.opencds.cqf.fhir.cr.implementationguide.KeyElementFilter;
import org.opencds.cqf.fhir.cr.implementationguide.KeyElementFilteringResult;
import org.opencds.cqf.fhir.utility.Libraries;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public class DataRequirementsVisitor extends BaseKnowledgeArtifactVisitor {
    protected DataRequirementsProcessor dataRequirementsProcessor;
    protected EvaluationSettings evaluationSettings;
    private PackageDownloader packageDownloader;
    private final List<IBaseResource> collectedResources;
    private final ImplementationGuidePackageResolver packageResolver;
    private final KeyElementFilter keyElementFilter;

    public DataRequirementsVisitor(IRepository repository, EvaluationSettings evaluationSettings) {
        super(repository);
        dataRequirementsProcessor = new DataRequirementsProcessor();
        this.evaluationSettings = evaluationSettings;
        this.collectedResources = new ArrayList<>();
        this.packageResolver = new ImplementationGuidePackageResolver(repository, fhirContext());
        this.keyElementFilter = new KeyElementFilter(repository);
    }

    /**
     * Sets the package downloader for testing purposes.
     * Public to allow tests to inject a mock downloader.
     */
    public void setPackageDownloader(PackageDownloader packageDownloader) {
        this.packageDownloader = packageDownloader;
        this.packageResolver.setPackageDownloader(packageDownloader);
    }

    /**
     * Returns the list of resources collected during $data-requirements processing.
     * This field is maintained for backwards compatibility but currently returns an empty list.
     * The persistDependencies feature was abandoned due to performance issues.
     *
     * @return Empty list (maintained for backwards compatibility)
     */
    public List<IBaseResource> getCollectedResources() {
        return new ArrayList<>(collectedResources);
    }

    /**
     * Internal context class to hold shared state during $data-requirements processing.
     * Encapsulates all the collections and metadata needed across different processing phases.
     */
    private static class DataRequirementsContext {
        final ILibraryAdapter library;
        final HashMap<String, IKnowledgeArtifactAdapter> gatheredResources;
        final HashMap<String, String> resourceSourcePackages;
        // Use wildcards to handle intersection types
        final HashMap<String, ? extends ICompositeType> allDependencies;
        final List<? extends ICompositeType> relatedArtifacts;
        final String mainIgCanonical;

        DataRequirementsContext(
                ILibraryAdapter library,
                HashMap<String, IKnowledgeArtifactAdapter> gatheredResources,
                HashMap<String, String> resourceSourcePackages,
                HashMap<String, ? extends ICompositeType> allDependencies,
                List<? extends ICompositeType> relatedArtifacts,
                String mainIgCanonical) {
            this.library = library;
            this.gatheredResources = gatheredResources;
            this.resourceSourcePackages = resourceSourcePackages;
            this.allDependencies = allDependencies;
            this.relatedArtifacts = relatedArtifacts;
            this.mainIgCanonical = mainIgCanonical;
        }
    }

    /**
     * Extracts operation parameters from the input.
     */
    private record OperationParameters(
            Optional<IBaseParameters> parameters,
            List<String> artifactVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion) {}

    private OperationParameters extractOperationParameters(IBaseParameters operationParameters) {
        Optional<IBaseParameters> parameters = VisitorHelper.getResourceParameter("parameters", operationParameters);
        List<String> artifactVersion = VisitorHelper.getStringListParameter("artifactVersion", operationParameters)
                .orElseGet(ArrayList::new);
        List<String> checkArtifactVersion = VisitorHelper.getStringListParameter(
                        "checkArtifactVersion", operationParameters)
                .orElseGet(ArrayList::new);
        List<String> forceArtifactVersion = VisitorHelper.getStringListParameter(
                        "forceArtifactVersion", operationParameters)
                .orElseGet(ArrayList::new);
        return new OperationParameters(parameters, artifactVersion, checkArtifactVersion, forceArtifactVersion);
    }

    /**
     * Initializes the module-definition Library and processes CQL data requirements if referenced libraries exist.
     * This step evaluates CQL libraries to extract their data requirements and adds them to the output library.
     */
    private ILibraryAdapter initializeLibraryWithCqlDataRequirements(
            IKnowledgeArtifactAdapter adapter, Optional<IBaseParameters> parameters) {
        var library = IAdapterFactory.forFhirContext(fhirContext())
                .createLibrary(fhirContext().getResourceDefinition("Library").newInstance());
        library.setName("EffectiveDataRequirements");
        library.setStatus(adapter.getStatus());
        library.setType("module-definition");

        var referencedLibraries = adapter.retrieveReferencedLibraries(repository);
        if (!referencedLibraries.isEmpty()) {
            var libraryManager = createLibraryManager();
            referencedLibraries.forEach((k, v) -> {
                var primaryLibrary =
                        referencedLibraries.values().stream().toList().get(0);
                CqlTranslator translator = translateLibrary(primaryLibrary.get(), libraryManager);
                var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(fhirContext());
                var evaluationParameters = parameters
                        .map(cqlFhirParametersConverter::toCqlParameters)
                        .orElse(null);

                var r5Library = dataRequirementsProcessor.gatherDataRequirements(
                        libraryManager,
                        translator.getTranslatedLibrary(),
                        evaluationSettings.getCqlOptions().getCqlCompilerOptions(),
                        null,
                        evaluationParameters,
                        true,
                        true);
                var convertedLibrary = convertAndCreateAdapter(r5Library);
                convertedLibrary.getDataRequirement().stream()
                        .map(IAdapter::get)
                        .map(ICompositeType.class::cast)
                        .forEach(library::addDataRequirement);
                convertedLibrary.getRelatedArtifact().forEach(library::addRelatedArtifact);
            });
        }
        return library;
    }

    /**
     * Initializes the data structures for tracking dependencies, gathered resources, and source packages.
     * Extracts all dependencies from the adapter before recursive gathering to identify unresolved dependencies.
     */
    @SuppressWarnings("unchecked")
    private DataRequirementsContext initializeDependencyTracking(
            IKnowledgeArtifactAdapter adapter, ILibraryAdapter library) {
        var gatheredResources = new HashMap<String, IKnowledgeArtifactAdapter>();
        var relatedArtifacts = stripInvalid(library);
        var resourceSourcePackages = new HashMap<String, String>();

        // Extract all dependencies before recursive gather to track unresolved ones
        var allDependencies = adapter.getRelatedArtifact().stream()
                .filter(ra -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(ra) != null)
                .peek(ra -> IAdapter.logger.debug(
                        "Found dependency before gather: {}",
                        IKnowledgeArtifactAdapter.getRelatedArtifactReference(ra)))
                .collect(java.util.stream.Collectors.toMap(
                        IKnowledgeArtifactAdapter::getRelatedArtifactReference,
                        ra -> ra,
                        (a, b) -> a)); // Keep first if duplicates
        IAdapter.logger.info("Total dependencies extracted: {}", allDependencies.size());

        String mainIgCanonical =
                adapter.hasVersion() ? adapter.getUrl() + "|" + adapter.getVersion() : adapter.getUrl();

        @SuppressWarnings("unchecked")
        HashMap<String, ? extends ICompositeType> allDepsWildcard = (HashMap) allDependencies;

        return new DataRequirementsContext(
                library,
                gatheredResources,
                resourceSourcePackages,
                allDepsWildcard,
                relatedArtifacts,
                mainIgCanonical);
    }

    /**
     * Filters and adds dependencies based on key element analysis.
     * Processes all gathered resources and dependencies, applying key element filtering to determine
     * which artifacts should be included in the final data requirements. Uses O(1) HashSet lookups
     * for performance optimization.
     */
    @SuppressWarnings("unchecked")
    private void filterAndAddDependencies(DataRequirementsContext context, KeyElementFilteringResult filteringResult) {
        IAdapter.logger.info("Filtering and adding dependencies");

        // Cast to mutable types for processing
        @SuppressWarnings({"rawtypes"})
        HashMap allDeps = (HashMap) context.allDependencies;
        @SuppressWarnings({"rawtypes"})
        List relatedArts = (List) context.relatedArtifacts;

        // Process gathered resources, applying key element filtering
        context.gatheredResources.values().forEach(r -> {
            String resourceType = r.get().fhirType();
            String canonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
            String canonicalNoVersion = r.getUrl();

            String sourcePackage = context.resourceSourcePackages.getOrDefault(
                    canonical, context.resourceSourcePackages.get(canonicalNoVersion));
            boolean isFromMainIG = context.mainIgCanonical.equals(sourcePackage);

            // Allow Libraries and CodeSystems from main IG, filter others via key element analysis
            if (resourceType.equals("Library")
                    || (resourceType.equals("CodeSystem") && isFromMainIG)
                    || filteringResult.shouldIncludeDependency(canonical, resourceType)) {

                // Resolved CodeSystems keep their package version. Truly external CodeSystems
                // (SNOMED, LOINC, etc.) that aren't resolved from any package are handled
                // separately in the external CodeSystems section below (lines ~416-453).
                var originalRa = allDeps.get(canonical);
                if (originalRa == null && !canonical.equals(canonicalNoVersion)) {
                    @SuppressWarnings("unchecked")
                    var entryStream = (java.util.stream.Stream<java.util.Map.Entry<String, Object>>)
                            allDeps.entrySet().stream();
                    originalRa = entryStream
                            .filter(e -> e.getKey().startsWith(canonicalNoVersion))
                            .map(java.util.Map.Entry::getValue)
                            .findFirst()
                            .orElse(null);
                }

                if (originalRa != null) {
                    String raReference = getRelatedArtifactReferenceUnchecked(originalRa);
                    boolean alreadyExists = false;
                    for (Object existing : relatedArts) {
                        String existingRef = getRelatedArtifactReferenceUnchecked(existing);
                        if (existingRef.equals(raReference)) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        relatedArts.add(originalRa);
                        IAdapter.logger.info(
                                "Added gathered resource with original relatedArtifact (has extension): {} -> {}",
                                canonical,
                                raReference);
                    }
                } else {
                    if (sourcePackage != null) {
                        addRelatedArtifactWithSourcePackage(relatedArts, r, sourcePackage, false);
                        IAdapter.logger.info(
                                "Added gathered resource from package {} with NEW sourcePackage extension: {}",
                                sourcePackage,
                                canonical);
                    } else {
                        addRelatedArtifact(relatedArts, r);
                        @SuppressWarnings("unchecked")
                        var allDepsKeys = (java.util.Set<String>) allDeps.keySet();
                        var sourcePkgKeys = context.resourceSourcePackages.keySet();
                        IAdapter.logger.warn(
                                "Added gathered resource WITHOUT sourcePackage extension (missing tracking): {} "
                                        + "[checked: exact='{}', noVersion='{}', allDeps keys={}, sourcePkg keys={}]",
                                canonical,
                                canonical,
                                canonicalNoVersion,
                                allDepsKeys.stream()
                                        .filter(k -> k.contains(canonicalNoVersion))
                                        .toList(),
                                sourcePkgKeys.stream()
                                        .filter(k -> k.contains(canonicalNoVersion))
                                        .toList());
                    }
                }
            } else {
                IAdapter.logger.debug("Excluding gathered resource (not key element terminology): {}", canonical);
            }
        });

        // Pre-compute sets for O(1) lookups (performance optimization)
        var resolvedCanonicals = new java.util.HashSet<String>();
        for (var r : context.gatheredResources.values()) {
            String resourceCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
            resolvedCanonicals.add(resourceCanonical);
            resolvedCanonicals.add(r.getUrl());
        }

        var addedRelatedArtifactCanonicals = new java.util.HashSet<String>();
        for (var ra : relatedArts) {
            String raCanonical = getRelatedArtifactReferenceUnchecked(ra);
            if (raCanonical != null) {
                addedRelatedArtifactCanonicals.add(raCanonical);
            }
        }

        // Process unresolved dependencies and IG relatedArtifacts
        var additionalDependencies = new HashMap<String, ICompositeType>();

        allDeps.forEach((canonical, ra) -> {
            var relArt = (ICompositeType & IBaseHasExtensions) ra;
            String canonicalStr = (String) canonical;
            String canonicalNoVersion = canonicalStr.split("\\|")[0];
            boolean wasResolved =
                    resolvedCanonicals.contains(canonicalStr) || resolvedCanonicals.contains(canonicalNoVersion);

            if (!wasResolved) {
                String relType = IKnowledgeArtifactAdapter.getRelatedArtifactType(relArt);
                boolean isIgDependency = "depends-on".equals(relType) && canonicalStr.contains("ImplementationGuide");

                if (isIgDependency) {
                    context.gatheredResources.values().stream()
                            .filter(r -> r.get().fhirType().equals("ImplementationGuide"))
                            .filter(r -> {
                                String igCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                                return canonicalStr.equals(igCanonical) || canonicalStr.startsWith(r.getUrl());
                            })
                            .findFirst()
                            .ifPresent(igAdapter -> {
                                igAdapter.getRelatedArtifact().stream()
                                        .filter(igRa ->
                                                IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa) != null)
                                        .forEach(igRa -> {
                                            String igRaCanonical =
                                                    IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa);
                                            String igRaResourceType =
                                                    extractResourceType(igRaCanonical, context.gatheredResources);
                                            if (!filteringResult.shouldIncludeDependency(
                                                    igRaCanonical, igRaResourceType)) {
                                                return;
                                            }

                                            if (!allDeps.containsKey(igRaCanonical)
                                                    && !additionalDependencies.containsKey(igRaCanonical)) {
                                                additionalDependencies.put(igRaCanonical, (ICompositeType) igRa);
                                                IAdapter.logger.info(
                                                        "Including key element dependency from fetched IG {}: {}",
                                                        canonicalStr,
                                                        igRaCanonical);
                                            }
                                        });
                            });
                } else {
                    if (!addedRelatedArtifactCanonicals.contains(canonicalStr)) {
                        String unresolvedResourceType = extractResourceType(canonicalStr, context.gatheredResources);
                        if (filteringResult.shouldIncludeDependency(canonicalStr, unresolvedResourceType)) {
                            relatedArts.add(relArt);
                            addedRelatedArtifactCanonicals.add(canonicalStr);
                            IAdapter.logger.info(
                                    "Including unresolved key element terminology dependency: {}", canonicalStr);
                        } else {
                            IAdapter.logger.debug("Excluding unresolved non-terminology dependency: {}", canonicalStr);
                        }
                    }
                }
            }
        });

        // Process additional dependencies from fetched IGs
        additionalDependencies.forEach((canonical, ra) -> {
            String canonicalNoVersion = canonical.split("\\|")[0];
            boolean wasResolved =
                    resolvedCanonicals.contains(canonical) || resolvedCanonicals.contains(canonicalNoVersion);

            if (!wasResolved && !addedRelatedArtifactCanonicals.contains(canonical)) {
                var raTyped = (ICompositeType & IBaseHasExtensions) ra;
                relatedArts.add(raTyped);
                addedRelatedArtifactCanonicals.add(canonical);
                IAdapter.logger.info("Including key element terminology dependency from fetched IG: {}", canonical);
            }
        });

        // Add external CodeSystems and ValueSets referenced by key element ValueSets
        // These are typically external terminologies (SNOMED, LOINC, etc.) that don't exist
        // as resources in packages but are referenced by ValueSets
        if (filteringResult.isFilteringEnabled()) {
            IAdapter.logger.info("Adding external terminologies referenced by key element ValueSets");

            // Add external CodeSystems
            for (String codeSystemUrl : filteringResult.getReferencedCodeSystems()) {
                if (!addedRelatedArtifactCanonicals.contains(codeSystemUrl)
                        && !resolvedCanonicals.contains(codeSystemUrl)) {
                    var externalCodeSystemRa = IKnowledgeArtifactAdapter.newRelatedArtifact(
                            fhirVersion(), "depends-on", codeSystemUrl, null);
                    addCqfResourceTypeExtension(externalCodeSystemRa, "CodeSystem");
                    relatedArts.add(externalCodeSystemRa);
                    addedRelatedArtifactCanonicals.add(codeSystemUrl);
                    IAdapter.logger.info("Added external CodeSystem: {}", codeSystemUrl);
                }
            }

            // Add external ValueSets
            for (String valueSetUrl : filteringResult.getReferencedValueSets()) {
                // Don't add if it's already a key element ValueSet
                String valueSetUrlNoVersion = valueSetUrl.split("\\|")[0];
                boolean isKeyElementVs = filteringResult.getKeyElementValueSets().stream()
                        .anyMatch(vs -> vs.equals(valueSetUrl) || vs.startsWith(valueSetUrlNoVersion));

                if (!isKeyElementVs
                        && !addedRelatedArtifactCanonicals.contains(valueSetUrl)
                        && !resolvedCanonicals.contains(valueSetUrl)
                        && !resolvedCanonicals.contains(valueSetUrlNoVersion)) {
                    var externalValueSetRa = IKnowledgeArtifactAdapter.newRelatedArtifact(
                            fhirVersion(), "depends-on", valueSetUrl, null);
                    addCqfResourceTypeExtension(externalValueSetRa, "ValueSet");
                    relatedArts.add(externalValueSetRa);
                    addedRelatedArtifactCanonicals.add(valueSetUrl);
                    IAdapter.logger.info("Added external ValueSet: {}", valueSetUrl);
                }
            }
        }

        // Post-processing: Ensure all relatedArtifacts have the cqf-resourceType extension.
        // Some code paths (unresolved dependencies, additionalDependencies from fetched IGs)
        // add relatedArtifacts without this extension. We fix them here using the
        // referencedCodeSystems/referencedValueSets sets and gatheredResources map.
        for (Object ra : relatedArts) {
            @SuppressWarnings("unchecked")
            var typedRa = (ICompositeType & IBaseHasExtensions) ra;
            if (hasCqfResourceTypeExtension(typedRa)) {
                continue;
            }
            String raCanonical = getRelatedArtifactReferenceUnchecked(ra);
            if (raCanonical == null) {
                continue;
            }
            String raCanonicalNoVersion = raCanonical.split("\\|")[0];

            // Determine resource type from filtering result sets or gathered resources
            String resourceType = null;
            if (filteringResult.getReferencedCodeSystems().contains(raCanonicalNoVersion)) {
                resourceType = "CodeSystem";
            } else if (filteringResult.getReferencedValueSets().contains(raCanonicalNoVersion)
                    || filteringResult.getKeyElementValueSets().stream()
                            .anyMatch(vs -> vs.equals(raCanonical) || vs.startsWith(raCanonicalNoVersion))) {
                resourceType = "ValueSet";
            } else {
                // Try to find in gathered resources
                resourceType = extractResourceType(raCanonical, context.gatheredResources);
            }

            if (resourceType != null) {
                addCqfResourceTypeExtension(typedRa, resourceType);
            }
        }
    }

    /**
     * Checks if a relatedArtifact already has the cqf-resourceType extension.
     */
    private <T extends ICompositeType & IBaseHasExtensions> boolean hasCqfResourceTypeExtension(T relatedArtifact) {
        String extensionUrl = org.opencds.cqf.fhir.utility.Constants.CQF_RESOURCETYPE;
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact dstu3Ra) {
            return dstu3Ra.getExtension().stream().anyMatch(e -> extensionUrl.equals(e.getUrl()));
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact r4Ra) {
            return r4Ra.getExtension().stream().anyMatch(e -> extensionUrl.equals(e.getUrl()));
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact r5Ra) {
            return r5Ra.getExtension().stream().anyMatch(e -> extensionUrl.equals(e.getUrl()));
        }
        return false;
    }

    /**
     * Adds the cqf-resourceType extension to a relatedArtifact.
     * This is needed for external terminologies whose URL patterns don't follow standard
     * FHIR canonical conventions (e.g., http://www.ada.org/cdt for a CodeSystem).
     */
    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & IBaseHasExtensions> void addCqfResourceTypeExtension(
            T relatedArtifact, String resourceType) {
        switch (fhirVersion()) {
            case DSTU3 -> ((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact)
                    .addExtension(new org.hl7.fhir.dstu3.model.Extension()
                            .setUrl(org.opencds.cqf.fhir.utility.Constants.CQF_RESOURCETYPE)
                            .setValue(new org.hl7.fhir.dstu3.model.CodeType(resourceType)));
            case R4 -> ((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact)
                    .addExtension(new org.hl7.fhir.r4.model.Extension()
                            .setUrl(org.opencds.cqf.fhir.utility.Constants.CQF_RESOURCETYPE)
                            .setValue(new org.hl7.fhir.r4.model.CodeType(resourceType)));
            case R5 -> ((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact)
                    .addExtension(new org.hl7.fhir.r5.model.Extension()
                            .setUrl(org.opencds.cqf.fhir.utility.Constants.CQF_RESOURCETYPE)
                            .setValue(new org.hl7.fhir.r5.model.CodeType(resourceType)));
            default -> IAdapter.logger.warn(
                    "Unsupported FHIR version for adding cqf-resourceType extension: {}", fhirVersion());
        }
    }

    /**
     * Processes $data-requirements for knowledge artifacts.
     *
     * <p>This operation extracts data requirements from CQL libraries and gathers all related artifacts
     * (Libraries, ValueSets, CodeSystems) needed for evaluation. The operation delegates to specialized
     * classes for resource-type-specific processing:</p>
     * <ul>
     *   <li>ImplementationGuide resources use {@link ImplementationGuidePackageResolver} for package
     *       fetching and {@link KeyElementFilter} for terminology filtering</li>
     *   <li>Other resource types (Library, PlanDefinition, Measure, Questionnaire, ValueSet, etc.) use
     *       recursive gathering to collect dependencies</li>
     * </ul>
     *
     * <p>Returns a Library resource with type "module-definition" containing dataRequirement and
     * relatedArtifact elements describing the data and terminology needed for evaluation.</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters operationParameters) {
        // Extract operation parameters
        var params = extractOperationParameters(operationParameters);

        // Initialize library with CQL data requirements
        var library = initializeLibraryWithCqlDataRequirements(adapter, params.parameters());

        // Initialize dependency tracking structures
        var context = initializeDependencyTracking(adapter, library);

        // Only use package resolution for ImplementationGuides
        KeyElementFilteringResult filteringResult;
        if (adapter.get().fhirType().equals("ImplementationGuide")) {
            // Use ImplementationGuidePackageResolver to fetch packages and dependencies
            var packageResult = packageResolver.resolvePackages(
                    adapter,
                    context.mainIgCanonical,
                    context.allDependencies,
                    this::recursiveGather,
                    params.artifactVersion(),
                    params.checkArtifactVersion(),
                    params.forceArtifactVersion());

            // Merge package resolution results into context
            context.gatheredResources.putAll(packageResult.getResources());
            context.resourceSourcePackages.putAll(packageResult.getResourceSourcePackages());

            // Use KeyElementFilter to identify key element ValueSets and CodeSystems
            filteringResult = keyElementFilter.analyzeKeyElements(
                    context.gatheredResources, context.resourceSourcePackages, context.mainIgCanonical);
        } else {
            // For non-IG resources, create a default filtering result with filtering disabled
            // This enables fallback mode: include all ValueSets and CodeSystems
            filteringResult = new KeyElementFilteringResult(
                    java.util.Collections.emptySet(),
                    java.util.Collections.emptySet(),
                    java.util.Collections.emptySet(),
                    false);
        }

        // Perform recursive gathering on the main adapter
        recursiveGather(
                adapter,
                context.gatheredResources,
                params.forceArtifactVersion(),
                params.forceArtifactVersion(),
                new ImmutableTriple<>(
                        params.artifactVersion(), params.checkArtifactVersion(), params.forceArtifactVersion()));

        // Phase 3: Filter and add dependencies based on key element analysis
        filterAndAddDependencies(context, filteringResult);

        // Finalize library with filtered relatedArtifacts
        var finalRelatedArts = (List) context.relatedArtifacts;
        context.library.setRelatedArtifact(finalRelatedArts);

        // Note: collectedResources population removed - the persistDependencies feature
        // was abandoned due to performance issues. The field and getter remain for
        // backwards compatibility but will return an empty list.

        return context.library.get();
    }

    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & IBaseHasExtensions> List<T> stripInvalid(ILibraryAdapter library) {
        // Until we support passing the Manifest we do not know the IG context and cannot correctly setup the
        // NamespaceManager
        // This method will strip out any relatedArtifacts that do not have a full valid canonical
        return library.getRelatedArtifact().stream()
                .filter(r -> {
                    var resourcePath = library.resolvePath(r, "resource");
                    var reference =
                            library.fhirContext().getVersion().getVersion().equals(FhirVersionEnum.DSTU3)
                                    ? ((org.hl7.fhir.dstu3.model.Reference) resourcePath).getReference()
                                    : ((IPrimitiveType<String>) resourcePath).getValue();
                    return reference.split("/").length > 2;
                })
                .map(r -> (T) r)
                .collect(Collectors.toList());
    }

    private ILibraryAdapter convertAndCreateAdapter(Library r5Library) {
        var adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion());
        return switch (fhirVersion()) {
            case DSTU3 -> {
                var versionConvertor3050 = new VersionConvertor_30_50(new BaseAdvisor_30_50());
                yield adapterFactory.createLibrary(versionConvertor3050.convertResource(r5Library));
            }
            case R4 -> {
                var versionConvertor4050 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
                yield adapterFactory.createLibrary(versionConvertor4050.convertResource(r5Library));
            }
            case R5 -> adapterFactory.createLibrary(r5Library);
            default -> throw new IllegalArgumentException(
                    "FHIR version %s is not supported.".formatted(fhirVersion().getFhirVersionString()));
        };
    }

    protected CqlTranslator getTranslator(InputStream cqlStream, LibraryManager libraryManager) {
        CqlTranslator translator;
        try {
            translator = CqlTranslator.fromSource(buffered(asSource(cqlStream)), libraryManager);
        } catch (IOException e) {
            throw new IllegalArgumentException("Errors occurred translating library: %s".formatted(e.getMessage()));
        }

        return translator;
    }

    protected CqlTranslator translateLibrary(IBaseResource library, LibraryManager libraryManager) {
        CqlTranslator translator = getTranslator(
                new ByteArrayInputStream(Libraries.getContent(library, "text/cql")
                        .orElseThrow(() -> new UnprocessableEntityException(
                                "No CQL content found for Library: %s".formatted(Libraries.getName(library))))),
                libraryManager);
        if (!translator.getErrors().isEmpty()) {
            throw new UnprocessableEntityException(translator.getErrors().get(0).getMessage());
        }
        return translator;
    }

    protected LibrarySourceProvider buildLibrarySource() {
        IAdapterFactory adapterFactory = IAdapterFactory.forFhirContext(fhirContext());
        return new RepositoryFhirLibrarySourceProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
    }

    protected LibraryManager createLibraryManager() {
        var librarySourceProvider = buildLibrarySource();
        var sourceProviders = new ArrayList<>(Arrays.asList(librarySourceProvider, librarySourceProvider));
        var modelManager = evaluationSettings.getModelCache() != null
                ? new ModelManager(evaluationSettings.getModelCache())
                : new ModelManager();
        var libraryManager = new LibraryManager(
                modelManager,
                evaluationSettings.getCqlOptions().getCqlCompilerOptions(),
                evaluationSettings.getLibraryCache());
        libraryManager.getLibrarySourceLoader().clearProviders();
        for (var provider : sourceProviders) {
            libraryManager.getLibrarySourceLoader().registerProvider(provider);
        }
        return libraryManager;
    }

    /**
     * Helper method to call getRelatedArtifactReference with proper unchecked suppression.
     * This is needed when working with raw types from DataRequirementsContext.
     */
    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & IBaseHasExtensions> String getRelatedArtifactReferenceUnchecked(Object ra) {
        return IKnowledgeArtifactAdapter.getRelatedArtifactReference((T) ra);
    }

    /**
     * Determines if a dependency should be included based on key element analysis.
     * Includes ValueSets bound to key elements and CodeSystems referenced by those ValueSets.
     * CodeSystems from the main IG are included automatically via the caller's check.
     *
     * <p>If keyElementValueSets is empty (no StructureDefinitions found), this method
     * falls back to including all ValueSet and CodeSystem resources.
     *
     * @param canonical the canonical URL of the dependency
     * @param resourceType the FHIR resource type (ValueSet, CodeSystem, etc.)
     * @param keyElementValueSets the set of ValueSet URLs bound to key elements (may be empty)
     * @param referencedCodeSystems pre-computed set of CodeSystem URLs referenced by key element ValueSets
     * @return true if the dependency should be included
     */
    private boolean shouldIncludeDependency(
            String canonical,
            String resourceType,
            java.util.Set<String> keyElementValueSets,
            java.util.Set<String> referencedCodeSystems) {
        if (canonical == null || canonical.isEmpty()) {
            return false;
        }

        // Only include ValueSet and CodeSystem resources
        if (resourceType == null || (!resourceType.equals("ValueSet") && !resourceType.equals("CodeSystem"))) {
            IAdapter.logger.debug("Excluding non-terminology dependency: {}", canonical);
            return false;
        }

        // Fallback mode: if no key element ValueSets were found (likely because StructureDefinitions
        // are not in the repository or package registry), include all ValueSets and CodeSystems
        if (keyElementValueSets.isEmpty()) {
            IAdapter.logger.debug(
                    "Key element filtering disabled - including all terminology resources: {}", canonical);
            return true;
        }

        // For ValueSets, check if it's a key element ValueSet
        if (resourceType.equals("ValueSet")) {
            String canonicalNoVersion = canonical.split("\\|")[0];
            boolean isKeyElement = keyElementValueSets.stream()
                    .anyMatch(vs -> vs.equals(canonical) || vs.startsWith(canonicalNoVersion));

            if (!isKeyElement) {
                IAdapter.logger.debug("Excluding ValueSet not bound to key elements: {}", canonical);
            }
            return isKeyElement;
        }

        // For CodeSystems, check if in the pre-computed set of referenced CodeSystems
        if (resourceType.equals("CodeSystem")) {
            String canonicalNoVersion = canonical.split("\\|")[0];
            boolean isReferenced = referencedCodeSystems.contains(canonicalNoVersion);

            if (!isReferenced) {
                IAdapter.logger.debug("Excluding CodeSystem not referenced by key element ValueSets: {}", canonical);
            } else {
                IAdapter.logger.debug("Including CodeSystem referenced by key element ValueSet: {}", canonical);
            }
            return isReferenced;
        }

        return false;
    }

    /**
     * Extracts the resource type for a given canonical URL.
     * First attempts to look up the actual resource in gatheredResources to get the true fhirType().
     * Falls back to URL pattern matching for unresolved dependencies.
     *
     * @param canonical the canonical URL (may include version with |)
     * @param gatheredResources all resources gathered from packages and repository
     * @return the resource type (e.g., "ValueSet", "CodeSystem"), or null if unknown
     */
    private String extractResourceType(
            String canonical, java.util.Map<String, IKnowledgeArtifactAdapter> gatheredResources) {
        if (canonical == null || canonical.isEmpty()) {
            return null;
        }

        // Strip version if present
        String canonicalNoVersion = canonical.split("\\|")[0];

        // First, try to find the actual resource in gatheredResources and get its fhirType()
        // This handles cases like http://hl7.org/fhir/goal-status (CodeSystem without /CodeSystem/ in URL)
        var resource = gatheredResources.values().stream()
                .filter(r -> {
                    String resourceCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                    return canonical.equals(resourceCanonical) || canonicalNoVersion.equals(r.getUrl());
                })
                .findFirst();

        if (resource.isPresent()) {
            return resource.get().get().fhirType();
        }

        // Fallback: Look for resource type in URL path (for unresolved dependencies)
        String[] pathParts = canonicalNoVersion.split("/");
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            // Common FHIR resource types we care about
            if (part.equals("ValueSet")
                    || part.equals("CodeSystem")
                    || part.equals("StructureDefinition")
                    || part.equals("SearchParameter")) {
                return part;
            }
        }

        return null;
    }

    /**
     * Adds a relatedArtifact for a gathered resource with the package-source extension.
     *
     * @param relatedArtifacts the list of relatedArtifacts to add to
     * @param adapter the resource adapter
     * @param sourcePackageCanonical the canonical URL of the source IG package
     * @param stripVersion if true, omit the version from the canonical reference (for external terminologies)
     */
    private <T extends ICompositeType & IBaseHasExtensions> void addRelatedArtifactWithSourcePackage(
            List<T> relatedArtifacts,
            IKnowledgeArtifactAdapter adapter,
            String sourcePackageCanonical,
            boolean stripVersion) {
        if (relatedArtifacts == null) {
            return;
        }

        var reference = (!stripVersion && adapter.hasVersion())
                ? adapter.getUrl().concat("|%s".formatted(adapter.getVersion()))
                : adapter.getUrl();

        // Check if already exists
        if (relatedArtifacts.stream().anyMatch(ra -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(ra)
                .equals(reference))) {
            return;
        }

        // Extract display from resource (prefer title, fallback to name)
        String display = adapter.hasTitle() ? adapter.getTitle() : adapter.getName();

        // Create new relatedArtifact with depends-on type (dependencies from external packages)
        T relatedArtifact =
                IKnowledgeArtifactAdapter.newRelatedArtifact(fhirVersion(), "depends-on", reference, display);

        // Add package-source extension based on FHIR version
        // This is a complex extension with packageId (required), version (optional), and uri (optional)
        if (sourcePackageCanonical != null && !sourcePackageCanonical.isEmpty()) {
            // Parse canonical to extract URL and version
            String[] parts = sourcePackageCanonical.split("\\|");
            String packageUrl = parts[0];
            String packageVersion = parts.length > 1 ? parts[1] : null;

            // Extract package ID from URL (last segment after last slash)
            String packageId = packageUrl.substring(packageUrl.lastIndexOf('/') + 1);

            switch (fhirVersion()) {
                case DSTU3 -> {
                    var extension = new org.hl7.fhir.dstu3.model.Extension();
                    extension.setUrl(org.opencds.cqf.fhir.utility.Constants.PACKAGE_SOURCE);

                    // Add required packageId sub-extension
                    var packageIdExt = new org.hl7.fhir.dstu3.model.Extension();
                    packageIdExt.setUrl("packageId");
                    packageIdExt.setValue(new org.hl7.fhir.dstu3.model.IdType(packageId));
                    extension.addExtension(packageIdExt);

                    // Add optional version sub-extension
                    if (packageVersion != null) {
                        var versionExt = new org.hl7.fhir.dstu3.model.Extension();
                        versionExt.setUrl("version");
                        versionExt.setValue(new org.hl7.fhir.dstu3.model.StringType(packageVersion));
                        extension.addExtension(versionExt);
                    }

                    // Add optional uri sub-extension
                    var uriExt = new org.hl7.fhir.dstu3.model.Extension();
                    uriExt.setUrl("uri");
                    uriExt.setValue(new org.hl7.fhir.dstu3.model.UriType(packageUrl));
                    extension.addExtension(uriExt);

                    ((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact).addExtension(extension);
                }
                case R4 -> {
                    var extension = new org.hl7.fhir.r4.model.Extension();
                    extension.setUrl(org.opencds.cqf.fhir.utility.Constants.PACKAGE_SOURCE);

                    // Add required packageId sub-extension
                    var packageIdExt = new org.hl7.fhir.r4.model.Extension();
                    packageIdExt.setUrl("packageId");
                    packageIdExt.setValue(new org.hl7.fhir.r4.model.IdType(packageId));
                    extension.addExtension(packageIdExt);

                    // Add optional version sub-extension
                    if (packageVersion != null) {
                        var versionExt = new org.hl7.fhir.r4.model.Extension();
                        versionExt.setUrl("version");
                        versionExt.setValue(new org.hl7.fhir.r4.model.StringType(packageVersion));
                        extension.addExtension(versionExt);
                    }

                    // Add optional uri sub-extension
                    var uriExt = new org.hl7.fhir.r4.model.Extension();
                    uriExt.setUrl("uri");
                    uriExt.setValue(new org.hl7.fhir.r4.model.UriType(packageUrl));
                    extension.addExtension(uriExt);

                    ((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact).addExtension(extension);
                }
                case R5 -> {
                    var extension = new org.hl7.fhir.r5.model.Extension();
                    extension.setUrl(org.opencds.cqf.fhir.utility.Constants.PACKAGE_SOURCE);

                    // Add required packageId sub-extension
                    var packageIdExt = new org.hl7.fhir.r5.model.Extension();
                    packageIdExt.setUrl("packageId");
                    packageIdExt.setValue(new org.hl7.fhir.r5.model.IdType(packageId));
                    extension.addExtension(packageIdExt);

                    // Add optional version sub-extension
                    if (packageVersion != null) {
                        var versionExt = new org.hl7.fhir.r5.model.Extension();
                        versionExt.setUrl("version");
                        versionExt.setValue(new org.hl7.fhir.r5.model.StringType(packageVersion));
                        extension.addExtension(versionExt);
                    }

                    // Add optional uri sub-extension
                    var uriExt = new org.hl7.fhir.r5.model.Extension();
                    uriExt.setUrl("uri");
                    uriExt.setValue(new org.hl7.fhir.r5.model.UriType(packageUrl));
                    extension.addExtension(uriExt);

                    ((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact).addExtension(extension);
                }
                default -> IAdapter.logger.warn(
                        "Unsupported FHIR version for adding package-source extension: {}", fhirVersion());
            }
        }

        // Add cqf-resourceType extension to indicate the resource type
        addCqfResourceTypeExtension(relatedArtifact, adapter.get().fhirType());

        relatedArtifacts.add(relatedArtifact);
    }
}
