package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.visitor.VisitorHelper.findUnsupportedCapability;
import static org.opencds.cqf.fhir.utility.visitor.VisitorHelper.processCanonicals;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Library;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Libraries;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.IKnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.visitor.VisitorHelper;

public class DataRequirementsVisitor implements IKnowledgeArtifactVisitor {
    protected DataRequirementsProcessor dataRequirementsProcessor;
    protected EvaluationSettings evaluationSettings;

    public DataRequirementsVisitor(EvaluationSettings evaluationSettings) {
        dataRequirementsProcessor = new DataRequirementsProcessor();
        this.evaluationSettings = evaluationSettings;
    }

    @Override
    public IBase visit(KnowledgeArtifactAdapter adapter, Repository repository, IBaseParameters drParameters) {
        var fhirVersion = adapter.get().getStructureFhirVersionEnum();
        Optional<IBaseParameters> parameters =
                VisitorHelper.getResourceParameter("parameters", drParameters, IBaseParameters.class);
        List<String> artifactVersion = VisitorHelper.getListParameter(
                        "artifactVersion", drParameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> checkArtifactVersion = VisitorHelper.getListParameter(
                        "checkArtifactVersion", drParameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> forceArtifactVersion = VisitorHelper.getListParameter(
                        "forceArtifactVersion", drParameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());

        LibraryAdapter library;
        var primaryLibrary = adapter.getPrimaryLibrary(repository);
        if (primaryLibrary != null) {
            var libraryManager = createLibraryManager(repository);
            CqlTranslator translator = translateLibrary(primaryLibrary, libraryManager);
            var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(repository.fhirContext());
            var evaluationParameters =
                    parameters.isPresent() ? cqlFhirParametersConverter.toCqlParameters(parameters.get()) : null;

            var r5Library = dataRequirementsProcessor.gatherDataRequirements(
                    libraryManager,
                    translator.getTranslatedLibrary(),
                    evaluationSettings.getCqlOptions().getCqlCompilerOptions(),
                    null,
                    evaluationParameters,
                    true,
                    true);
            library = convertAndCreateAdapter(fhirVersion, r5Library);
        } else {
            library = AdapterFactory.forFhirContext(repository.fhirContext())
                    .createLibrary(repository
                            .fhirContext()
                            .getResourceDefinition("Library")
                            .newInstance());
            library.setName("EffectiveDataRequirements");
            library.setStatus(adapter.getStatus());
            library.setType("module-definition");
        }
        var gatheredResources = new ArrayList<String>();
        var relatedArtifacts = stripInvalid(library);
        recursiveGather(
                adapter.get(),
                gatheredResources,
                relatedArtifacts,
                repository,
                forceArtifactVersion,
                forceArtifactVersion,
                artifactVersion,
                checkArtifactVersion,
                forceArtifactVersion);

        library.setRelatedArtifact(relatedArtifacts);
        return library.get();
    }

    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & IBaseHasExtensions> List<T> stripInvalid(LibraryAdapter library) {
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

    private LibraryAdapter convertAndCreateAdapter(FhirVersionEnum fhirVersion, Library r5Library) {
        var adapterFactory = AdapterFactory.forFhirVersion(fhirVersion);
        switch (fhirVersion) {
            case DSTU3:
                var versionConvertor3050 = new VersionConvertor_30_50(new BaseAdvisor_30_50());
                return adapterFactory.createLibrary(versionConvertor3050.convertResource(r5Library));
            case R4:
                var versionConvertor4050 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
                return adapterFactory.createLibrary(versionConvertor4050.convertResource(r5Library));
            case R5:
                return adapterFactory.createLibrary(r5Library);

            default:
                throw new IllegalArgumentException(
                        String.format("FHIR version %s is not supported.", fhirVersion.toString()));
        }
    }

    protected CqlTranslator getTranslator(InputStream cqlStream, LibraryManager libraryManager) {
        CqlTranslator translator;
        try {
            translator = CqlTranslator.fromStream(cqlStream, libraryManager);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Errors occurred translating library: %s", e.getMessage()));
        }

        return translator;
    }

    protected CqlTranslator translateLibrary(IBaseResource library, LibraryManager libraryManager) {
        CqlTranslator translator = getTranslator(
                new ByteArrayInputStream(
                        Libraries.getContent(library, "text/cql").get()),
                libraryManager);
        if (!translator.getErrors().isEmpty()) {
            throw new IllegalArgumentException(translator.getErrors().get(0).getMessage());
        }
        return translator;
    }

    protected static LibrarySourceProvider buildLibrarySource(Repository repository) {
        AdapterFactory adapterFactory = AdapterFactory.forFhirContext(repository.fhirContext());
        return new RepositoryFhirLibrarySourceProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
    }

    protected LibraryManager createLibraryManager(Repository repository) {
        var librarySourceProvider = buildLibrarySource(repository);
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

    protected <T extends ICompositeType & IBaseHasExtensions> void recursiveGather(
            IDomainResource resource,
            List<String> gatheredResources,
            List<T> relatedArtifacts,
            Repository repository,
            List<String> capability,
            List<String> include,
            List<String> artifactVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion)
            throws PreconditionFailedException {
        if (resource != null && !gatheredResources.contains(resource.getId())) {
            gatheredResources.add(resource.getId());
            var fhirVersion = resource.getStructureFhirVersionEnum();
            var adapter = AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(resource);
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, artifactVersion, checkArtifactVersion, forceArtifactVersion);
            var reference = adapter.hasVersion()
                    ? adapter.getUrl().concat(String.format("|%s", adapter.getVersion()))
                    : adapter.getUrl();
            boolean addArtifact = relatedArtifacts.stream()
                    .noneMatch(ra -> KnowledgeArtifactAdapter.getRelatedArtifactReference(ra)
                            .equals(reference));
            if (addArtifact) {
                relatedArtifacts.add(KnowledgeArtifactAdapter.newRelatedArtifact(
                        fhirVersion, "depends-on", reference, adapter.getDescriptor()));
            }

            adapter.combineComponentsAndDependencies().stream()
                    // sometimes VS dependencies aren't FHIR resources
                    .filter(ra -> StringUtils.isNotBlank(ra.getReference())
                            && StringUtils.isNotBlank(Canonicals.getResourceType(ra.getReference())))
                    .filter(ra -> {
                        try {
                            var resourceDef = repository
                                    .fhirContext()
                                    .getResourceDefinition(Canonicals.getResourceType(ra.getReference()));
                            return resourceDef != null;
                        } catch (DataFormatException e) {
                            if (e.getMessage().contains("1684")) {
                                return false;
                            } else {
                                throw new DataFormatException(e.getMessage());
                            }
                        }
                    })
                    .map(ra -> SearchHelper.searchRepositoryByCanonicalWithPaging(repository, ra.getReference()))
                    .map(searchBundle -> (IDomainResource) BundleHelper.getEntryResourceFirstRep(searchBundle))
                    .forEach(component -> recursiveGather(
                            component,
                            gatheredResources,
                            relatedArtifacts,
                            repository,
                            capability,
                            include,
                            artifactVersion,
                            checkArtifactVersion,
                            forceArtifactVersion));
        }
    }
}
