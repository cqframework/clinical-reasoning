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
import org.opencds.cqf.fhir.utility.Libraries;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public class DataRequirementsVisitor extends BaseKnowledgeArtifactVisitor {
    protected DataRequirementsProcessor dataRequirementsProcessor;
    protected EvaluationSettings evaluationSettings;
    private PackageDownloader packageDownloader;
    private List<IBaseResource> collectedResources;

    public DataRequirementsVisitor(IRepository repository, EvaluationSettings evaluationSettings) {
        super(repository);
        dataRequirementsProcessor = new DataRequirementsProcessor();
        this.evaluationSettings = evaluationSettings;
        this.collectedResources = new ArrayList<>();
    }

    /**
     * Sets the package downloader for testing purposes.
     * Public to allow tests to inject a mock downloader.
     */
    public void setPackageDownloader(PackageDownloader packageDownloader) {
        this.packageDownloader = packageDownloader;
    }

    /**
     * Returns the list of resources collected during $data-requirements processing.
     * This includes all resources from the ImplementationGuide package(s) that were
     * fetched during dependency resolution.
     *
     * @return List of collected resources, or empty list if none collected
     */
    public List<IBaseResource> getCollectedResources() {
        return new ArrayList<>(collectedResources);
    }

    /**
     * Processes $data-requirements for ImplementationGuides.
     *
     * <p><strong>Implementation Note:</strong> For ImplementationGuide resources, this operation focuses
     * exclusively on ValueSet and CodeSystem dependencies. When resolving dependencies from package
     * registry IGs, only ValueSets and CodeSystems that are bound to key elements (mustSupport,
     * differential elements, mandatory children, slices, modifiers, etc.) in the IG's StructureDefinitions
     * are included. Other resource types (StructureDefinitions, SearchParameters, CapabilityStatements, etc.)
     * from dependency IGs are excluded to keep the data requirements focused on terminology resources
     * actually needed for data validation and exchange.</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters operationParameters) {
        Optional<IBaseParameters> parameters = VisitorHelper.getResourceParameter("parameters", operationParameters);
        List<String> artifactVersion = VisitorHelper.getStringListParameter("artifactVersion", operationParameters)
                .orElseGet(ArrayList::new);
        List<String> checkArtifactVersion = VisitorHelper.getStringListParameter(
                        "checkArtifactVersion", operationParameters)
                .orElseGet(ArrayList::new);
        List<String> forceArtifactVersion = VisitorHelper.getStringListParameter(
                        "forceArtifactVersion", operationParameters)
                .orElseGet(ArrayList::new);

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
        var gatheredResources = new HashMap<String, IKnowledgeArtifactAdapter>();
        var relatedArtifacts = stripInvalid(library);

        // Track source package for each resource (canonical URL → source package canonical)
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
        IAdapter.logger.info("Total dependencies extracted from IG: {}", allDependencies.size());

        // Fetch package resources for the MAIN IG first (before recursive gather)
        // This ensures we have ValueSets, CodeSystems, and StructureDefinitions from the main IG package
        String mainIgCanonical =
                adapter.hasVersion() ? adapter.getUrl() + "|" + adapter.getVersion() : adapter.getUrl();
        IAdapter.logger.info("Fetching package resources for main IG: {}", mainIgCanonical);
        var mainIgPackageResources = packageDownloader != null
                ? PackageHelper.fetchPackageResources(
                        mainIgCanonical,
                        fhirContext(),
                        IAdapterFactory.forFhirContext(fhirContext()),
                        repository,
                        packageDownloader)
                : PackageHelper.fetchPackageResources(
                        mainIgCanonical, fhirContext(), IAdapterFactory.forFhirContext(fhirContext()), repository);

        // Add main IG package resources to gathered resources and track source package
        mainIgPackageResources.forEach((resourceCanonicalUrl, resourceAdapter) -> {
            if (!gatheredResources.containsKey(resourceCanonicalUrl)) {
                gatheredResources.put(resourceCanonicalUrl, resourceAdapter);
                resourceSourcePackages.put(resourceCanonicalUrl, mainIgCanonical);
                IAdapter.logger.debug(
                        "Added {} from main IG package to gathered resources",
                        resourceAdapter.get().fhirType());
            }
        });
        IAdapter.logger.info(
                "Added {} resources from main IG package. Total gathered resources: {}",
                mainIgPackageResources.size(),
                gatheredResources.size());

        recursiveGather(
                adapter,
                gatheredResources,
                forceArtifactVersion,
                forceArtifactVersion,
                new ImmutableTriple<>(artifactVersion, checkArtifactVersion, forceArtifactVersion));

        // PHASE 1: Fetch unresolved ImplementationGuide dependencies from package registry
        // This ensures we have their StructureDefinitions before doing key element analysis
        IAdapter.logger.info("Phase 1: Fetching unresolved ImplementationGuide dependencies from package registry");

        // Collect new relatedArtifacts from fetched IGs to add after iteration
        @SuppressWarnings("unchecked")
        var newRelatedArtifacts = new HashMap<String, ICompositeType>();

        allDependencies.forEach((canonical, ra) -> {
            boolean wasResolved = gatheredResources.values().stream().anyMatch(r -> {
                String resourceCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                return canonical.equals(resourceCanonical) || canonical.startsWith(r.getUrl());
            });

            if (!wasResolved) {
                // Check if this is an ImplementationGuide dependency
                String relType = IKnowledgeArtifactAdapter.getRelatedArtifactType(ra);
                boolean isIgDependency = "depends-on".equals(relType) && canonical.contains("ImplementationGuide");

                if (isIgDependency) {
                    // Try to fetch from package registry
                    IAdapter.logger.info(
                            "Attempting to fetch unresolved ImplementationGuide from package registry: {}", canonical);
                    var igResource = PackageHelper.fetchImplementationGuideFromRegistry(canonical, fhirContext());

                    IKnowledgeArtifactAdapter igAdapter = null;
                    String igCanonical = null;

                    if (igResource != null) {
                        // Successfully fetched IG
                        igAdapter = IAdapterFactory.forFhirContext(fhirContext())
                                .createKnowledgeArtifactAdapter(igResource);
                        IAdapter.logger.info(
                                "Successfully fetched IG from registry, now fetching all package resources: {}",
                                canonical);

                        // Add IG to gathered resources
                        igCanonical = igAdapter.hasVersion()
                                ? igAdapter.getUrl() + "|" + igAdapter.getVersion()
                                : igAdapter.getUrl();
                        gatheredResources.put(igCanonical, igAdapter);
                    } else {
                        // No IG found - this is expected for terminology packages like VSAC/PHINVADS
                        IAdapter.logger.info(
                                "No ImplementationGuide found in package (expected for terminology packages like VSAC/PHINVADS), "
                                        + "but will still fetch package resources: {}",
                                canonical);
                    }

                    // Track the source package canonical (either from IG or the dependency canonical)
                    // Must be final for use in lambda
                    final String sourcePackageCanonical = igCanonical != null ? igCanonical : canonical;

                    // Fetch ALL resources from the package (ValueSets, CodeSystems, StructureDefinitions, etc.)
                    // This works even if there's no IG resource in the package
                    var packageResources = packageDownloader != null
                            ? PackageHelper.fetchPackageResources(
                                    canonical,
                                    fhirContext(),
                                    IAdapterFactory.forFhirContext(fhirContext()),
                                    repository,
                                    packageDownloader)
                            : PackageHelper.fetchPackageResources(
                                    canonical,
                                    fhirContext(),
                                    IAdapterFactory.forFhirContext(fhirContext()),
                                    repository);

                    // Add all package resources to gathered resources and track source package
                    packageResources.forEach((resourceCanonicalUrl, resourceAdapter) -> {
                        if (!gatheredResources.containsKey(resourceCanonicalUrl)) {
                            gatheredResources.put(resourceCanonicalUrl, resourceAdapter);
                            resourceSourcePackages.put(resourceCanonicalUrl, sourcePackageCanonical);
                            IAdapter.logger.debug(
                                    "Added {} from package {} to gathered resources",
                                    resourceAdapter.get().fhirType(),
                                    canonical);
                        }
                    });

                    IAdapter.logger.info(
                            "Added {} resources from package {}. Total gathered resources now: {}",
                            packageResources.size(),
                            canonical,
                            gatheredResources.size());

                    // If we have an IG, collect its relatedArtifacts and process recursively
                    if (igAdapter != null) {
                        // Collect relatedArtifacts from fetched IG (which have package-source extensions)
                        // Store them temporarily to avoid ConcurrentModificationException
                        igAdapter.getRelatedArtifact().stream()
                                .filter(igRa -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa) != null)
                                .forEach(igRa -> {
                                    String raCanonical = IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa);
                                    // Only add if not already present (don't overwrite)
                                    if (!allDependencies.containsKey(raCanonical)
                                            && !newRelatedArtifacts.containsKey(raCanonical)) {
                                        newRelatedArtifacts.put(raCanonical, (ICompositeType) igRa);
                                        IAdapter.logger.debug(
                                                "Collected relatedArtifact from fetched IG: {}", raCanonical);
                                    }
                                });

                        // Process IG recursively to get its dependencies
                        recursiveGather(
                                igAdapter,
                                gatheredResources,
                                forceArtifactVersion,
                                forceArtifactVersion,
                                new ImmutableTriple<>(artifactVersion, checkArtifactVersion, forceArtifactVersion));

                        IAdapter.logger.info(
                                "Completed gathering resources from fetched IG: {}. Total gathered resources now: {}",
                                canonical,
                                gatheredResources.size());
                    }
                }
            }
        });

        // Add collected relatedArtifacts from fetched IGs to allDependencies
        if (!newRelatedArtifacts.isEmpty()) {
            newRelatedArtifacts.forEach(
                    (canonical, ra) -> allDependencies.put(canonical, (ICompositeType & IBaseHasExtensions) ra));
            IAdapter.logger.info(
                    "Added {} relatedArtifacts from fetched IGs to allDependencies. Total: {}",
                    newRelatedArtifacts.size(),
                    allDependencies.size());
        }

        // PHASE 2: Identify key element ValueSets from ALL gathered StructureDefinitions
        // (including those from fetched IGs)
        IAdapter.logger.info("Phase 2: Analyzing key elements from gathered StructureDefinitions");
        var keyElementValueSets = identifyKeyElementValueSets(gatheredResources);

        if (keyElementValueSets.isEmpty()) {
            IAdapter.logger.warn(
                    "No key element ValueSets found in gathered StructureDefinitions. "
                            + "Key element filtering will be disabled - all ValueSets and CodeSystems from dependency IGs will be included. "
                            + "This typically means the IG's StructureDefinitions are not available in the repository or package registry.");
        } else {
            IAdapter.logger.info(
                    "Key element filtering enabled. Found {} key element ValueSets from {} StructureDefinitions.",
                    keyElementValueSets.size(),
                    gatheredResources.values().stream()
                            .filter(r -> r.get().fhirType().equals("StructureDefinition"))
                            .count());
        }

        // PHASE 3: Filter and add dependencies based on key element analysis
        IAdapter.logger.info("Phase 3: Filtering and adding dependencies based on key element analysis");

        // Add gathered resources as relatedArtifacts, applying key element filtering
        // Only include ValueSet/CodeSystem resources (exclude ImplementationGuides, SearchParameters, etc.)
        // Preserve original relatedArtifacts from allDependencies (which have package-source extensions)
        gatheredResources.values().forEach(r -> {
            String resourceType = r.get().fhirType();
            String canonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
            String canonicalNoVersion = r.getUrl(); // URL without version for fallback matching

            // Allow Libraries (from main IG), but filter other resource types
            if (resourceType.equals("Library") || shouldIncludeDependency(canonical, keyElementValueSets)) {
                // Check if we have the original relatedArtifact from allDependencies (which has extensions)
                // Try exact match first, then try URL-only match
                var originalRa = allDependencies.get(canonical);
                if (originalRa == null && !canonical.equals(canonicalNoVersion)) {
                    // Try matching without version
                    originalRa = allDependencies.entrySet().stream()
                            .filter(e -> e.getKey().startsWith(canonicalNoVersion))
                            .map(java.util.Map.Entry::getValue)
                            .findFirst()
                            .orElse(null);
                }

                if (originalRa != null) {
                    // Use the original relatedArtifact (preserves package-source extension)
                    String raReference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(originalRa);
                    if (relatedArtifacts.stream()
                            .noneMatch(existing -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(existing)
                                    .equals(raReference))) {
                        relatedArtifacts.add((ICompositeType & IBaseHasExtensions) originalRa);
                        IAdapter.logger.info(
                                "Added gathered resource with original relatedArtifact (has extension): {} -> {}",
                                canonical,
                                raReference);
                    }
                } else {
                    // No original relatedArtifact - check if we know the source package (from package fetch)
                    String sourcePackage = resourceSourcePackages.get(canonical);
                    if (sourcePackage == null && !canonical.equals(canonicalNoVersion)) {
                        // Try matching without version
                        sourcePackage = resourceSourcePackages.get(canonicalNoVersion);
                    }

                    if (sourcePackage != null) {
                        // Create new relatedArtifact with package-source extension
                        addRelatedArtifactWithSourcePackage(relatedArtifacts, r, sourcePackage);
                        IAdapter.logger.info(
                                "Added gathered resource from package {} with NEW sourcePackage extension: {}",
                                sourcePackage,
                                canonical);
                    } else {
                        // No source package info - create basic relatedArtifact (shouldn't happen often)
                        addRelatedArtifact(relatedArtifacts, r);
                        IAdapter.logger.warn(
                                "Added gathered resource WITHOUT sourcePackage extension (missing tracking): {} "
                                        + "[checked: exact='{}', noVersion='{}', allDeps keys={}, sourcePkg keys={}]",
                                canonical,
                                canonical,
                                canonicalNoVersion,
                                allDependencies.keySet().stream()
                                        .filter(k -> k.contains(canonicalNoVersion))
                                        .toList(),
                                resourceSourcePackages.keySet().stream()
                                        .filter(k -> k.contains(canonicalNoVersion))
                                        .toList());
                    }
                }
            } else {
                IAdapter.logger.debug("Excluding gathered resource (not key element terminology): {}", canonical);
            }
        });

        // Collect additional dependencies from fetched IGs
        @SuppressWarnings("unchecked")
        var additionalDependencies = new HashMap<String, ICompositeType>();

        allDependencies.forEach((canonical, ra) -> {
            boolean wasResolved = gatheredResources.values().stream().anyMatch(r -> {
                String resourceCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                return canonical.equals(resourceCanonical) || canonical.startsWith(r.getUrl());
            });

            if (!wasResolved) {
                // Check if this is an IG dependency (already fetched in Phase 1)
                String relType = IKnowledgeArtifactAdapter.getRelatedArtifactType(ra);
                boolean isIgDependency = "depends-on".equals(relType) && canonical.contains("ImplementationGuide");

                if (isIgDependency) {
                    // IG was already fetched in Phase 1, get its adapter from gatheredResources
                    gatheredResources.values().stream()
                            .filter(r -> r.get().fhirType().equals("ImplementationGuide"))
                            .filter(r -> {
                                String igCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                                return canonical.equals(igCanonical) || canonical.startsWith(r.getUrl());
                            })
                            .findFirst()
                            .ifPresent(igAdapter -> {
                                // Collect the fetched IG's dependencies and filter them
                                igAdapter.getRelatedArtifact().stream()
                                        .filter(igRa ->
                                                IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa) != null)
                                        .forEach(igRa -> {
                                            String igRaCanonical =
                                                    IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa);

                                            // Apply key element filtering
                                            if (!shouldIncludeDependency(igRaCanonical, keyElementValueSets)) {
                                                return; // Skip this dependency
                                            }

                                            // Only add if not already tracked
                                            if (!allDependencies.containsKey(igRaCanonical)
                                                    && !additionalDependencies.containsKey(igRaCanonical)) {
                                                additionalDependencies.put(igRaCanonical, (ICompositeType) igRa);
                                                IAdapter.logger.info(
                                                        "Including key element dependency from fetched IG {}: {}",
                                                        canonical,
                                                        igRaCanonical);
                                            }
                                        });
                            });
                } else {
                    // Non-IG unresolved dependency - apply filtering
                    if (relatedArtifacts.stream()
                            .noneMatch(existing -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(existing)
                                    .equals(canonical))) {

                        // Apply filtering: only include ValueSet/CodeSystem
                        if (shouldIncludeDependency(canonical, keyElementValueSets)) {
                            relatedArtifacts.add((ICompositeType & IBaseHasExtensions) ra);
                            IAdapter.logger.info(
                                    "Including unresolved key element terminology dependency: {}", canonical);
                        } else {
                            IAdapter.logger.debug("Excluding unresolved non-terminology dependency: {}", canonical);
                        }
                    }
                }
            }
        });

        // Process additional dependencies from fetched IGs
        additionalDependencies.forEach((canonical, ra) -> {
            boolean wasResolved = gatheredResources.values().stream().anyMatch(r -> {
                String resourceCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                return canonical.equals(resourceCanonical) || canonical.startsWith(r.getUrl());
            });

            if (!wasResolved) {
                // Add as unresolved dependency (already filtered for key elements)
                if (relatedArtifacts.stream()
                        .noneMatch(existing -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(existing)
                                .equals(canonical))) {
                    relatedArtifacts.add((ICompositeType & IBaseHasExtensions) ra);
                    IAdapter.logger.info("Including key element terminology dependency from fetched IG: {}", canonical);
                }
            }
        });

        library.setRelatedArtifact(relatedArtifacts);

        // Collect resources from gatheredResources for potential persistence
        collectedResources.clear();
        gatheredResources.values().forEach(resourceAdapter -> collectedResources.add(resourceAdapter.get()));
        IAdapter.logger.info("Collected {} resources for potential persistence", collectedResources.size());

        return library.get();
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
     * Identifies ValueSets that are bound to key elements in the gathered StructureDefinitions.
     * Uses KeyElementAnalyzer to determine which ValueSets are actually needed based on
     * mustSupport, differential elements, mandatory children, slices, modifiers, etc.
     *
     * @param gatheredResources the resources gathered from the main IG
     * @return set of ValueSet canonical URLs that are bound to key elements
     */
    private java.util.Set<String> identifyKeyElementValueSets(
            java.util.Map<String, IKnowledgeArtifactAdapter> gatheredResources) {
        var keyElementValueSets = new java.util.HashSet<String>();
        var analyzer = new KeyElementAnalyzer(repository);

        for (var resourceAdapter : gatheredResources.values()) {
            var resource = resourceAdapter.get();
            // Only analyze StructureDefinitions
            if (resource.fhirType().equals("StructureDefinition")) {
                var valueSets = analyzer.getKeyElementValueSets(resource);
                keyElementValueSets.addAll(valueSets);
                IAdapter.logger.debug(
                        "Found {} key element ValueSets in StructureDefinition: {}",
                        valueSets.size(),
                        resourceAdapter.getUrl());
            }
        }

        IAdapter.logger.info("Total key element ValueSets identified: {}", keyElementValueSets.size());
        return keyElementValueSets;
    }

    /**
     * Determines if a dependency should be included based on key element analysis.
     * Only includes ValueSet and CodeSystem resources. For ValueSets, they must be
     * identified as key elements. For CodeSystems, they are included if their canonical
     * URL appears in any of the key element ValueSet URLs (as ValueSets often reference
     * CodeSystems with matching URLs).
     *
     * <p>If keyElementValueSets is empty (no StructureDefinitions found), this method
     * falls back to including all ValueSet and CodeSystem resources.
     *
     * @param canonical the canonical URL of the dependency
     * @param keyElementValueSets the set of ValueSet URLs bound to key elements (may be empty)
     * @return true if the dependency should be included
     */
    private boolean shouldIncludeDependency(String canonical, java.util.Set<String> keyElementValueSets) {
        if (canonical == null || canonical.isEmpty()) {
            return false;
        }

        // Extract resource type from canonical URL
        String resourceType = extractResourceType(canonical);

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
            // Strip version from canonical for comparison
            String canonicalNoVersion = canonical.split("\\|")[0];
            boolean isKeyElement = keyElementValueSets.stream()
                    .anyMatch(vs -> vs.equals(canonical) || vs.startsWith(canonicalNoVersion));

            if (!isKeyElement) {
                IAdapter.logger.debug("Excluding ValueSet not bound to key elements: {}", canonical);
            }
            return isKeyElement;
        }

        // For CodeSystems, include if the base URL appears in any key element ValueSet
        // (ValueSets often reference CodeSystems from the same package)
        if (resourceType.equals("CodeSystem")) {
            String canonicalNoVersion = canonical.split("\\|")[0];

            // Extract base URL (everything before "/CodeSystem/")
            int codeSystemIndex = canonicalNoVersion.lastIndexOf("/CodeSystem/");
            if (codeSystemIndex == -1) {
                // CodeSystem URL doesn't follow the expected pattern (e.g., http://hl7.org/fhir/sid/icd-10-cm)
                // For these non-standard URLs, we'll be more permissive and just check if the domain matches
                IAdapter.logger.debug(
                        "CodeSystem has non-standard URL pattern, using permissive matching: {}", canonical);

                // Extract domain (e.g., "hl7.org" from "http://hl7.org/fhir/sid/icd-10-cm")
                String domain = extractDomain(canonicalNoVersion);
                if (domain == null) {
                    IAdapter.logger.debug("Cannot extract domain from CodeSystem URL: {}", canonical);
                    return false;
                }

                // Check if any key element ValueSet shares the same domain
                boolean isReferenced = keyElementValueSets.stream().anyMatch(vs -> {
                    String vsDomain = extractDomain(vs.split("\\|")[0]);
                    return vsDomain != null && vsDomain.equals(domain);
                });

                if (!isReferenced) {
                    IAdapter.logger.debug(
                            "Excluding CodeSystem (non-standard URL) not referenced by key element ValueSets: {}",
                            canonical);
                }
                return isReferenced;
            }

            String baseUrl = canonicalNoVersion.substring(0, codeSystemIndex);
            boolean isReferenced = keyElementValueSets.stream().anyMatch(vs -> vs.startsWith(baseUrl));

            if (!isReferenced) {
                IAdapter.logger.debug("Excluding CodeSystem not referenced by key element ValueSets: {}", canonical);
            }
            return isReferenced;
        }

        return false;
    }

    /**
     * Extracts the resource type from a canonical URL.
     * Examples:
     *   "http://example.org/ValueSet/my-vs" -> "ValueSet"
     *   "http://example.org/CodeSystem/my-cs|1.0" -> "CodeSystem"
     *
     * @param canonical the canonical URL
     * @return the resource type, or null if not found
     */
    private String extractResourceType(String canonical) {
        if (canonical == null || canonical.isEmpty()) {
            return null;
        }

        // Strip version if present
        String urlWithoutVersion = canonical.split("\\|")[0];

        // Look for resource type in URL path
        String[] pathParts = urlWithoutVersion.split("/");
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
     * Extracts the domain from a URL.
     * Examples:
     *   "http://hl7.org/fhir/ValueSet/observation-status" -> "hl7.org"
     *   "http://terminology.hl7.org/CodeSystem/v3-ActCode" -> "terminology.hl7.org"
     *
     * @param url the URL
     * @return the domain, or null if not found
     */
    private String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            // Remove protocol (http:// or https://)
            String withoutProtocol = url.replaceFirst("^https?://", "");

            // Extract domain (everything before the first slash)
            int slashIndex = withoutProtocol.indexOf('/');
            if (slashIndex > 0) {
                return withoutProtocol.substring(0, slashIndex);
            } else {
                return withoutProtocol;
            }
        } catch (Exception e) {
            IAdapter.logger.debug("Error extracting domain from URL: {}", url, e);
            return null;
        }
    }

    /**
     * Adds a relatedArtifact for a gathered resource with the package-source extension.
     *
     * @param relatedArtifacts the list of relatedArtifacts to add to
     * @param adapter the resource adapter
     * @param sourcePackageCanonical the canonical URL of the source IG package
     */
    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & IBaseHasExtensions> void addRelatedArtifactWithSourcePackage(
            List<T> relatedArtifacts, IKnowledgeArtifactAdapter adapter, String sourcePackageCanonical) {
        if (relatedArtifacts == null) {
            return;
        }

        var reference = adapter.hasVersion()
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

        relatedArtifacts.add(relatedArtifact);
    }
}
