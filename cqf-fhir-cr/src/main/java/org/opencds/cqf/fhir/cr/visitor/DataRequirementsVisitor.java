package org.opencds.cqf.fhir.cr.visitor;

import static kotlinx.io.CoreKt.buffered;
import static kotlinx.io.JvmCoreKt.asSource;
import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Library;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.cr.common.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Libraries;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.client.terminology.ArtifactEndpointConfiguration;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyProviderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataRequirementsVisitor extends BaseKnowledgeArtifactVisitor {
    private static final Logger logger = LoggerFactory.getLogger(DataRequirementsVisitor.class);

    protected DataRequirementsProcessor dataRequirementsProcessor;
    protected EvaluationSettings evaluationSettings;
    protected final ITerminologyProviderRouter terminologyProviderRouter;

    public DataRequirementsVisitor(IRepository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null);
    }

    public DataRequirementsVisitor(
            IRepository repository,
            EvaluationSettings evaluationSettings,
            ITerminologyProviderRouter terminologyProviderRouter) {
        super(repository);
        dataRequirementsProcessor = new DataRequirementsProcessor();
        this.evaluationSettings = evaluationSettings;
        this.terminologyProviderRouter = terminologyProviderRouter;
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters operationParameters) {
        Optional<IBaseParameters> parameters = VisitorHelper.getResourceParameter("parameters", operationParameters);
        var artifactVersion = VisitorHelper.getStringListParameter("artifactVersion", operationParameters)
                .orElseGet(ArrayList::new);
        var checkArtifactVersion = VisitorHelper.getStringListParameter("checkArtifactVersion", operationParameters)
                .orElseGet(ArrayList::new);
        var forceArtifactVersion = VisitorHelper.getStringListParameter("forceArtifactVersion", operationParameters)
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
                var translator = translateLibrary(primaryLibrary.get(), libraryManager);
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

        var relatedArtifacts = stripInvalid(library);

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "DataRequirementsVisitor.visit: terminologyProviderRouter={}, adapter type={}",
                    terminologyProviderRouter != null
                            ? terminologyProviderRouter.getClass().getSimpleName()
                            : "null",
                    adapter.get().fhirType());
        }

        if (terminologyProviderRouter != null) {
            // Enriched path: dependency classification, Tx queries, CRMI extensions
            var artifactEndpointConfigurations = VisitorHelper.getArtifactEndpointConfigurations(operationParameters);
            var terminologyEndpoint = VisitorHelper.getResourceParameter("terminologyEndpoint", operationParameters)
                    .map(r -> (IEndpointAdapter) createAdapterForResource(r));

            // Create conformance resource resolver for key element analysis
            var dependsOnPackages = extractDependsOnPackages(adapter);
            logger.debug("Extracted {} dependsOn package(s) from IG", dependsOnPackages.size());
            for (var pkg : dependsOnPackages) {
                logger.debug("  Package seed: {}#{}", pkg[0], pkg[1]);
            }
            var conformanceResolver =
                    new ConformanceResourceResolver(repository, dependsOnPackages, artifactEndpointConfigurations);
            var federatedRepo = conformanceResolver.getFederatedRepository();
            logger.debug("Federated repo type: {}", federatedRepo.getClass().getSimpleName());

            // Pre-compute transitive key dependencies from ValueSet compose chains
            var transitiveKeyCanonicals = computeTransitiveKeyCanonicals(adapter, conformanceResolver, federatedRepo);

            var ctx = new GatherContext(
                    new HashSet<>(),
                    new HashMap<>(),
                    artifactEndpointConfigurations,
                    terminologyEndpoint.orElse(null),
                    conformanceResolver,
                    federatedRepo,
                    transitiveKeyCanonicals);

            gatherDependenciesWithClassification(adapter, new ArrayList<>(), relatedArtifacts, ctx);

            // Add root IG as a composed-of entry
            if (adapter.get() instanceof org.hl7.fhir.r4.model.ImplementationGuide
                    || adapter.get() instanceof org.hl7.fhir.r5.model.ImplementationGuide) {
                var igComposedOf = IKnowledgeArtifactAdapter.newRelatedArtifact(
                        fhirVersion(), "composed-of", adapter.getCanonical(), adapter.getDescriptor());
                relatedArtifacts.add(igComposedOf);
            }
        } else {
            // Original path: simple recursiveGather (Library, Measure, etc.)
            var gatheredResources = new HashMap<String, IKnowledgeArtifactAdapter>();
            recursiveGather(
                    adapter,
                    gatheredResources,
                    forceArtifactVersion,
                    forceArtifactVersion,
                    new ImmutableTriple<>(artifactVersion, checkArtifactVersion, forceArtifactVersion));
            gatheredResources.values().forEach(r -> addRelatedArtifact(relatedArtifacts, r));
        }

        library.setRelatedArtifact(relatedArtifacts);
        return library.get();
    }

    private record GatherContext(
            Set<String> gatheredCanonicals,
            Map<String, IKnowledgeArtifactAdapter> resolvedCache,
            List<ArtifactEndpointConfiguration> endpointConfigurations,
            IEndpointAdapter terminologyEndpoint,
            ConformanceResourceResolver conformanceResolver,
            IRepository dependencyRepo,
            Set<String> transitiveKeyCanonicals) {}

    private <T extends ICompositeType & IBaseHasExtensions> void gatherDependenciesWithClassification(
            IKnowledgeArtifactAdapter artifactAdapter,
            List<String> parentRoles,
            List<T> relatedArtifacts,
            GatherContext ctx) {
        if (artifactAdapter == null) {
            return;
        }
        var canonical = artifactAdapter.getCanonical();
        if (!ctx.gatheredCanonicals.add(canonical)) {
            return;
        }

        var dependencies = artifactAdapter.getDependencies(ctx.dependencyRepo);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "gatherDependenciesWithClassification: {} returned {} dependencies",
                    artifactAdapter.get().fhirType(),
                    dependencies.size());
        }
        for (var dependency : dependencies) {
            processDependency(dependency, artifactAdapter, parentRoles, relatedArtifacts, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & IBaseHasExtensions> void processDependency(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter artifactAdapter,
            List<String> parentRoles,
            List<T> relatedArtifacts,
            GatherContext ctx) {
        var dependencyUrl = Canonicals.getUrl(dependency.getReference());
        if (StringUtils.isBlank(dependencyUrl)) {
            dependencyUrl = dependency.getReference();
        }
        if (StringUtils.isBlank(dependencyUrl)) {
            return;
        }

        // Resolve dependency
        var dependencyAdapter = ctx.resolvedCache.computeIfAbsent(
                dependencyUrl,
                k -> tryResolveDependencyReadOnly(
                        dependency, ctx.endpointConfigurations, ctx.terminologyEndpoint, ctx.dependencyRepo));

        // Classify roles (includes transitive key canonicals from compose walking)
        var currentRoles = DependencyRoleClassifier.classifyDependencyRoles(
                dependency, artifactAdapter, dependencyAdapter, ctx.conformanceResolver, ctx.transitiveKeyCanonicals);
        if (parentRoles.contains("key") && !currentRoles.contains("key")) {
            currentRoles.add(0, "key");
        }

        // Recurse into resolved dependency
        if (dependencyAdapter != null) {
            gatherDependenciesWithClassification(dependencyAdapter, currentRoles, relatedArtifacts, ctx);
        }

        var reference = enrichReference(dependency, dependencyAdapter, ctx.conformanceResolver);

        // Skip if already in the related artifacts list
        if (relatedArtifacts.stream().anyMatch(ra -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(ra)
                .equals(reference))) {
            return;
        }

        var newDep = (T) IKnowledgeArtifactAdapter.newRelatedArtifact(
                fhirVersion(),
                Constants.RELATEDARTIFACT_TYPE_DEPENDSON,
                reference,
                dependencyAdapter != null ? dependencyAdapter.getDescriptor() : null);

        // Add CRMI extensions
        addCrmiExtensions(newDep, dependency, dependencyAdapter, currentRoles, ctx.conformanceResolver);

        relatedArtifacts.add(newDep);
    }

    private String enrichReference(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter dependencyAdapter,
            ConformanceResourceResolver conformanceResolver) {
        if (dependencyAdapter != null) {
            return dependencyAdapter.getCanonical();
        }
        var reference = dependency.getReference();
        if (StringUtils.isBlank(Canonicals.getVersion(reference))) {
            var indexedVersion = conformanceResolver.getVersion(reference);
            if (indexedVersion != null) {
                return Canonicals.getUrl(reference) + "|" + indexedVersion;
            }
        }
        return reference;
    }

    private IKnowledgeArtifactAdapter tryResolveDependencyReadOnly(
            IDependencyInfo dependency,
            List<ArtifactEndpointConfiguration> artifactEndpointConfigurations,
            IEndpointAdapter terminologyEndpoint,
            IRepository dependencyRepo) {
        var reference = dependency.getReference();

        // First try resolving from the federated repository
        if (StringUtils.isNotBlank(Canonicals.getVersion(reference))) {
            var adapter = getArtifactByCanonical(reference, dependencyRepo);
            if (adapter != null) {
                return adapter;
            }
        } else {
            var maybeAdapter = VisitorHelper.tryGetLatestVersion(reference, dependencyRepo);
            if (maybeAdapter.isPresent()) {
                return maybeAdapter.get();
            }
        }

        // Fall back to Tx server for unversioned ValueSets and CodeSystems
        return tryResolveFromTerminologyServer(reference, artifactEndpointConfigurations, terminologyEndpoint);
    }

    private IKnowledgeArtifactAdapter tryResolveFromTerminologyServer(
            String reference,
            List<ArtifactEndpointConfiguration> artifactEndpointConfigurations,
            IEndpointAdapter terminologyEndpoint) {
        var resourceType = Canonicals.getResourceType(reference);
        if (resourceType == null || terminologyProviderRouter == null) {
            return null;
        }

        var url = Canonicals.getUrl(reference);
        Optional<IDomainResource> txResult;

        if (!artifactEndpointConfigurations.isEmpty()) {
            txResult = queryTerminologyWithConfigurations(resourceType, artifactEndpointConfigurations, url);
        } else if (terminologyEndpoint != null) {
            txResult = queryTerminologyWithEndpoint(resourceType, terminologyEndpoint, url);
        } else {
            return null;
        }

        return txResult.map(r -> (IKnowledgeArtifactAdapter) createAdapterForResource(r))
                .orElse(null);
    }

    private Optional<IDomainResource> queryTerminologyWithConfigurations(
            String resourceType, List<ArtifactEndpointConfiguration> configurations, String url) {
        if (Constants.RESOURCETYPE_VALUESET.equals(resourceType)) {
            return terminologyProviderRouter.getValueSetResourceWithConfigurations(configurations, url);
        } else if (Constants.RESOURCETYPE_CODESYSTEM.equals(resourceType)) {
            return terminologyProviderRouter.getCodeSystemResourceWithConfigurations(configurations, url);
        }
        return Optional.empty();
    }

    private Optional<IDomainResource> queryTerminologyWithEndpoint(
            String resourceType, IEndpointAdapter endpoint, String url) {
        if (Constants.RESOURCETYPE_VALUESET.equals(resourceType)) {
            return terminologyProviderRouter.getLatestValueSetResource(List.of(endpoint), url);
        } else if (Constants.RESOURCETYPE_CODESYSTEM.equals(resourceType)) {
            return terminologyProviderRouter.getCodeSystemResource(List.of(endpoint), url);
        }
        return Optional.empty();
    }

    private IKnowledgeArtifactAdapter getArtifactByCanonical(String canonical, IRepository dependencyRepo) {
        try {
            var bundle = SearchHelper.searchRepositoryByCanonicalWithPaging(dependencyRepo, canonical);
            if (bundle != null) {
                var resource =
                        IKnowledgeArtifactAdapter.findLatestVersion(bundle).orElse(null);
                if (resource != null) {
                    return (IKnowledgeArtifactAdapter) createAdapterForResource(resource);
                }
            }
        } catch (Exception e) {
            logger.debug("Error resolving canonical: {}", canonical, e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & IBaseHasExtensions> void addCrmiExtensions(
            T relatedArtifact,
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter dependencyArtifact,
            List<String> roles,
            ConformanceResourceResolver conformanceResolver) {
        var extensionList = (List<IBaseExtension<?, ?>>) relatedArtifact.getExtension();

        // cqf-resourceType extension — fallback chain:
        // 1. Resolved resource fhirType() (most accurate)
        // 2. NPM package index + NamingSystem-derived set (for unresolved deps)
        // 3. Skip (don't add garbage)
        String resourceType = null;
        if (dependencyArtifact != null) {
            resourceType = dependencyArtifact.get().fhirType();
        } else if (conformanceResolver != null) {
            resourceType = conformanceResolver.getResourceType(dependency.getReference());
        }
        if (resourceType != null) {
            addResourceTypeExtension(resourceType, relatedArtifact);
        }

        // crmi-dependencyRole extensions
        for (var role : roles) {
            extensionList.add(ExtensionBuilders.buildDependencyRoleExt(fhirVersion(), role));
        }

        // package-source extension (complex) — use NPM package index
        var depUrl = dependencyArtifact != null ? dependencyArtifact.getUrl() : dependency.getReference();
        if (conformanceResolver != null && depUrl != null) {
            var pkgInfo = conformanceResolver.getPackageInfo(depUrl);
            if (pkgInfo != null) {
                extensionList.add(ExtensionBuilders.buildComplexPackageSourceExt(
                        fhirVersion(), pkgInfo.packageId(), pkgInfo.version(), pkgInfo.canonical()));
            }
        }

        // crmi-referenceSource extensions
        for (var path : dependency.getFhirPaths()) {
            if (dependency.getReferenceSource() != null) {
                extensionList.add(ExtensionBuilders.buildReferenceSourceExt(
                        fhirVersion(), dependency.getReferenceSource(), path));
            }
        }
    }

    private void addResourceTypeExtension(String resourceType, ICompositeType relatedArtifact) {
        if (fhirVersion().equals(FhirVersionEnum.R4)) {
            ((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact)
                    .getResourceElement()
                    .addExtension(Constants.CQF_RESOURCETYPE, new org.hl7.fhir.r4.model.CodeType(resourceType));
        } else if (fhirVersion().equals(FhirVersionEnum.R5)) {
            ((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact)
                    .getResourceElement()
                    .addExtension(Constants.CQF_RESOURCETYPE, new org.hl7.fhir.r5.model.CodeType(resourceType));
        }
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
            default ->
                throw new IllegalArgumentException("FHIR version %s is not supported."
                        .formatted(fhirVersion().getFhirVersionString()));
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

    /**
     * Extracts package ID + version pairs from an IG, including the root IG's own package
     * and all dependsOn entries. The root IG's package is included first so that its own
     * definition resources can be resolved from the NPM cache when not in the repository.
     * NpmRepository walks transitive dependencies automatically, so dependsOn entries are
     * included as explicit seeds for redundancy.
     */
    /**
     * Pre-computes the set of transitive key canonical URLs by scanning all StructureDefinitions
     * in the IG for key element ValueSet bindings, then walking those ValueSets' compose chains
     * to discover referenced CodeSystems and transitive ValueSets.
     */
    private Set<String> computeTransitiveKeyCanonicals(
            IKnowledgeArtifactAdapter igAdapter,
            ConformanceResourceResolver conformanceResolver,
            IRepository federatedRepo) {
        var allKeyValueSets = new HashSet<String>();
        var analyzer = new KeyElementAnalyzer(conformanceResolver, fhirVersion());

        // Scan all dependencies to find StructureDefinitions and extract their key ValueSets
        for (var dependency : igAdapter.getDependencies(federatedRepo)) {
            var reference = dependency.getReference();
            if (reference == null) {
                continue;
            }
            var resourceType = conformanceResolver.getResourceType(Canonicals.getUrl(reference));
            if (!"StructureDefinition".equals(resourceType)) {
                continue;
            }
            var sd = conformanceResolver.resolveStructureDefinition(Canonicals.getUrl(reference));
            if (sd != null) {
                try {
                    allKeyValueSets.addAll(analyzer.getKeyElementValueSets(sd));
                } catch (Exception e) {
                    logger.debug("Error analyzing key elements for {}: {}", reference, e.getMessage());
                }
            }
        }

        if (allKeyValueSets.isEmpty()) {
            return Set.of();
        }

        logger.debug("Found {} key ValueSets from profile bindings, walking compose chains", allKeyValueSets.size());

        // Walk compose chains to discover transitive CodeSystems and ValueSets
        var composeWalker = new ValueSetComposeWalker(conformanceResolver, fhirVersion());
        var composeResult = composeWalker.walkComposeChains(allKeyValueSets);

        // Build unified set of all transitive key canonicals
        var transitiveKeyCanonicals = new HashSet<String>();
        transitiveKeyCanonicals.addAll(allKeyValueSets);
        transitiveKeyCanonicals.addAll(composeResult.transitiveValueSets());
        transitiveKeyCanonicals.addAll(composeResult.transitiveCodeSystems());

        logger.debug(
                "Transitive key canonicals: {} total ({} from profile bindings, {} transitive ValueSets, {} transitive CodeSystems)",
                transitiveKeyCanonicals.size(),
                allKeyValueSets.size(),
                composeResult.transitiveValueSets().size(),
                composeResult.transitiveCodeSystems().size());

        return transitiveKeyCanonicals;
    }

    private List<String[]> extractDependsOnPackages(IKnowledgeArtifactAdapter adapter) {
        List<String[]> packages = new ArrayList<>();
        try {
            if (adapter.get() instanceof org.hl7.fhir.r4.model.ImplementationGuide ig) {
                extractR4DependsOnPackages(ig, packages);
            } else if (adapter.get() instanceof org.hl7.fhir.r5.model.ImplementationGuide ig) {
                extractR5DependsOnPackages(ig, packages);
            }
        } catch (Exception e) {
            logger.debug("Error extracting dependsOn packages", e);
        }
        return packages;
    }

    private void extractR4DependsOnPackages(org.hl7.fhir.r4.model.ImplementationGuide ig, List<String[]> packages) {
        if (ig.hasPackageId() && ig.hasVersion()) {
            packages.add(new String[] {ig.getPackageId(), ig.getVersion()});
        }
        for (var dep : ig.getDependsOn()) {
            if (dep.hasPackageId() && dep.hasVersion()) {
                packages.add(new String[] {dep.getPackageId(), dep.getVersion()});
            }
        }
    }

    private void extractR5DependsOnPackages(org.hl7.fhir.r5.model.ImplementationGuide ig, List<String[]> packages) {
        if (ig.hasPackageId() && ig.hasVersion()) {
            packages.add(new String[] {ig.getPackageId(), ig.getVersion()});
        }
        for (var dep : ig.getDependsOn()) {
            if (dep.hasPackageId() && dep.hasVersion()) {
                packages.add(new String[] {dep.getPackageId(), dep.getVersion()});
            }
        }
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
}
