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

        if (terminologyProviderRouter != null) {
            // Enriched path: dependency classification, Tx queries, CRMI extensions
            var artifactEndpointConfigurations = VisitorHelper.getArtifactEndpointConfigurations(operationParameters);
            var terminologyEndpoint = VisitorHelper.getResourceParameter("terminologyEndpoint", operationParameters)
                    .map(r -> (IEndpointAdapter) createAdapterForResource(r));

            var gatheredCanonicals = new HashSet<String>();
            var resolvedCache = new HashMap<String, IKnowledgeArtifactAdapter>();

            gatherDependenciesWithClassification(
                    adapter,
                    adapter,
                    gatheredCanonicals,
                    resolvedCache,
                    artifactEndpointConfigurations,
                    terminologyEndpoint.orElse(null),
                    new ArrayList<>(),
                    relatedArtifacts);
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

    private <T extends ICompositeType & IBaseHasExtensions> void gatherDependenciesWithClassification(
            IKnowledgeArtifactAdapter rootAdapter,
            IKnowledgeArtifactAdapter artifactAdapter,
            Set<String> gatheredCanonicals,
            Map<String, IKnowledgeArtifactAdapter> resolvedCache,
            List<ArtifactEndpointConfiguration> artifactEndpointConfigurations,
            IEndpointAdapter terminologyEndpoint,
            List<String> parentRoles,
            List<T> relatedArtifacts) {
        if (artifactAdapter == null) {
            return;
        }
        var canonical = artifactAdapter.getCanonical();
        if (gatheredCanonicals.contains(canonical)) {
            return;
        }
        gatheredCanonicals.add(canonical);

        var dependencies = artifactAdapter.getDependencies(repository);
        for (var dependency : dependencies) {
            var dependencyUrl = Canonicals.getUrl(dependency.getReference());
            if (StringUtils.isBlank(dependencyUrl)) {
                dependencyUrl = dependency.getReference();
            }
            if (StringUtils.isBlank(dependencyUrl)) {
                continue;
            }

            // Resolve dependency
            IKnowledgeArtifactAdapter dependencyAdapter;
            if (resolvedCache.containsKey(dependencyUrl)) {
                dependencyAdapter = resolvedCache.get(dependencyUrl);
            } else {
                dependencyAdapter =
                        tryResolveDependencyReadOnly(dependency, artifactEndpointConfigurations, terminologyEndpoint);
                resolvedCache.put(dependencyUrl, dependencyAdapter);
            }

            // Classify roles
            var currentRoles = DependencyRoleClassifier.classifyDependencyRoles(
                    dependency, artifactAdapter, dependencyAdapter, repository);
            if (parentRoles.contains("key") && !currentRoles.contains("key")) {
                currentRoles.add(0, "key");
            }

            // Recurse into resolved dependency
            if (dependencyAdapter != null) {
                gatherDependenciesWithClassification(
                        rootAdapter,
                        dependencyAdapter,
                        gatheredCanonicals,
                        resolvedCache,
                        artifactEndpointConfigurations,
                        terminologyEndpoint,
                        currentRoles,
                        relatedArtifacts);
            }

            // Only add dependencies from leaf artifacts (not the root)
            if (!artifactAdapter.getUrl().equals(rootAdapter.getUrl())) {
                var reference = dependency.getReference();
                if (dependencyAdapter != null) {
                    reference = dependencyAdapter.getCanonical();
                }

                // Skip if already in the related artifacts list
                var finalReference = reference;
                if (relatedArtifacts.stream().anyMatch(ra -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(ra)
                        .equals(finalReference))) {
                    continue;
                }

                var newDep = (T) IKnowledgeArtifactAdapter.newRelatedArtifact(
                        fhirVersion(),
                        Constants.RELATEDARTIFACT_TYPE_DEPENDSON,
                        reference,
                        dependencyAdapter != null ? dependencyAdapter.getDescriptor() : null);

                // Add CRMI extensions
                addCrmiExtensions(newDep, dependency, artifactAdapter, dependencyAdapter, currentRoles);

                relatedArtifacts.add(newDep);
            }
        }
    }

    private IKnowledgeArtifactAdapter tryResolveDependencyReadOnly(
            IDependencyInfo dependency,
            List<ArtifactEndpointConfiguration> artifactEndpointConfigurations,
            IEndpointAdapter terminologyEndpoint) {
        var reference = dependency.getReference();
        var resourceType = Canonicals.getResourceType(reference);

        // First try resolving from the local repository
        if (StringUtils.isNotBlank(Canonicals.getVersion(reference))) {
            var adapter = getArtifactByCanonical(reference);
            if (adapter != null) {
                return adapter;
            }
        } else {
            var maybeAdapter = VisitorHelper.tryGetLatestVersion(reference, repository);
            if (maybeAdapter.isPresent()) {
                return maybeAdapter.get();
            }
        }

        // Fall back to Tx server for unversioned ValueSets and CodeSystems
        if (resourceType != null && terminologyProviderRouter != null) {
            Optional<IDomainResource> txResult = Optional.empty();

            if (!artifactEndpointConfigurations.isEmpty()) {
                // Use configuration-based routing
                if (Constants.RESOURCETYPE_VALUESET.equals(resourceType)) {
                    txResult = terminologyProviderRouter.getValueSetResourceWithConfigurations(
                            artifactEndpointConfigurations, Canonicals.getUrl(reference));
                } else if (Constants.RESOURCETYPE_CODESYSTEM.equals(resourceType)) {
                    txResult = terminologyProviderRouter.getCodeSystemResourceWithConfigurations(
                            artifactEndpointConfigurations, Canonicals.getUrl(reference));
                }
            } else if (terminologyEndpoint != null) {
                // Use single endpoint
                if (Constants.RESOURCETYPE_VALUESET.equals(resourceType)) {
                    txResult = terminologyProviderRouter.getLatestValueSetResource(
                            List.of(terminologyEndpoint), Canonicals.getUrl(reference));
                } else if (Constants.RESOURCETYPE_CODESYSTEM.equals(resourceType)) {
                    txResult = terminologyProviderRouter.getCodeSystemResource(
                            List.of(terminologyEndpoint), Canonicals.getUrl(reference));
                }
            }

            if (txResult.isPresent()) {
                return (IKnowledgeArtifactAdapter) createAdapterForResource(txResult.get());
            }
        }

        return null;
    }

    private IKnowledgeArtifactAdapter getArtifactByCanonical(String canonical) {
        try {
            var bundle = SearchHelper.searchRepositoryByCanonicalWithPaging(repository, canonical);
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
            IKnowledgeArtifactAdapter sourceArtifact,
            IKnowledgeArtifactAdapter dependencyArtifact,
            List<String> roles) {
        var extensionList = (List<IBaseExtension<?, ?>>) relatedArtifact.getExtension();

        // cqf-resourceType extension
        var resourceType = Canonicals.getResourceType(dependency.getReference());
        if (resourceType != null) {
            addResourceTypeExtension(resourceType, relatedArtifact);
        }

        // crmi-dependencyRole extensions
        for (var role : roles) {
            extensionList.add(ExtensionBuilders.buildDependencyRoleExt(fhirVersion(), role));
        }

        // package-source extension (complex)
        if (dependencyArtifact != null) {
            PackageSourceResolver.resolvePackageSource(dependencyArtifact, repository)
                    .ifPresent(packageSource -> {
                        String packageId;
                        String version;
                        if (packageSource.contains("#")) {
                            packageId = packageSource.substring(0, packageSource.indexOf('#'));
                            version = packageSource.substring(packageSource.indexOf('#') + 1);
                        } else {
                            packageId = packageSource;
                            version = null;
                        }
                        extensionList.add(ExtensionBuilders.buildComplexPackageSourceExt(
                                fhirVersion(), packageId, version, dependencyArtifact.getUrl()));
                    });
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
