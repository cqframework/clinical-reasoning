package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
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
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.Libraries;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public class DataRequirementsVisitor extends BaseKnowledgeArtifactVisitor {
    protected DataRequirementsProcessor dataRequirementsProcessor;
    protected EvaluationSettings evaluationSettings;

    public DataRequirementsVisitor(Repository repository, EvaluationSettings evaluationSettings) {
        super(repository);
        dataRequirementsProcessor = new DataRequirementsProcessor();
        this.evaluationSettings = evaluationSettings;
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters operationParameters) {
        Optional<IBaseParameters> parameters = VisitorHelper.getResourceParameter("parameters", operationParameters);
        List<String> artifactVersion = VisitorHelper.getStringListParameter("artifactVersion", operationParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> checkArtifactVersion = VisitorHelper.getStringListParameter(
                        "checkArtifactVersion", operationParameters)
                .orElseGet(() -> new ArrayList<>());
        List<String> forceArtifactVersion = VisitorHelper.getStringListParameter(
                        "forceArtifactVersion", operationParameters)
                .orElseGet(() -> new ArrayList<>());

        ILibraryAdapter library;
        var primaryLibrary = adapter.getPrimaryLibrary(repository);
        if (primaryLibrary != null) {
            var libraryManager = createLibraryManager();
            CqlTranslator translator = translateLibrary(primaryLibrary, libraryManager);
            var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(fhirContext());
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
            library = convertAndCreateAdapter(r5Library);
        } else {
            library = IAdapterFactory.forFhirContext(fhirContext())
                    .createLibrary(
                            fhirContext().getResourceDefinition("Library").newInstance());
            library.setName("EffectiveDataRequirements");
            library.setStatus(adapter.getStatus());
            library.setType("module-definition");
        }
        var gatheredResources = new HashMap<String, IKnowledgeArtifactAdapter>();
        var relatedArtifacts = stripInvalid(library);
        recursiveGather(
                adapter,
                gatheredResources,
                forceArtifactVersion,
                forceArtifactVersion,
                new ImmutableTriple<>(artifactVersion, checkArtifactVersion, forceArtifactVersion));
        gatheredResources.values().stream().forEach(r -> addRelatedArtifact(relatedArtifacts, r));
        library.setRelatedArtifact(relatedArtifacts);
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
        switch (fhirVersion()) {
            case DSTU3:
                var versionConvertor3050 = new VersionConvertor_30_50(new BaseAdvisor_30_50());
                return adapterFactory.createLibrary(versionConvertor3050.convertResource(r5Library));
            case R4:
                var versionConvertor4050 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
                return adapterFactory.createLibrary(versionConvertor4050.convertResource(r5Library));
            case R5:
                return adapterFactory.createLibrary(r5Library);

            default:
                throw new IllegalArgumentException(String.format(
                        "FHIR version %s is not supported.", fhirVersion().getFhirVersionString()));
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
