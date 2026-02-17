package org.opencds.cqf.fhir.cr.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.SearchHelper.readRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.TestOperationProvider;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class TestCql {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

    private static InputStream open(String asset) {
        var path = Path.of(getResourcePath(TestCql.class) + "/" + CLASS_PATH + "/" + asset);
        var file = path.toFile();
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String load(String asset) throws IOException {
        return load(open(asset));
    }

    public static Given given() {
        return new Given();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class Given {
        private IRepository repository;
        private EvaluationSettings evaluationSettings;

        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        public CqlProcessor buildProcessor(IRepository repository) {
            if (repository instanceof IgRepository igRepository) {
                igRepository.setOperationProvider(TestOperationProvider.newProvider(repository.fhirContext()));
            }
            if (evaluationSettings == null) {
                evaluationSettings = EvaluationSettings.getDefault();
                evaluationSettings
                        .getRetrieveSettings()
                        .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                        .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

                evaluationSettings
                        .getTerminologySettings()
                        .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
            }
            var crSettings = CrSettings.getDefault().withEvaluationSettings(evaluationSettings);
            return new CqlProcessor(repository, crSettings);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class When {
        private final IRepository repository;
        private final CqlProcessor processor;
        private final IParser jsonParser;

        private String subject;
        private String expression;
        private boolean useServerData;
        private IRepository dataRepository;
        private IRepository contentRepository;
        private IRepository terminologyRepository;
        private IBaseBundle additionalData;
        private List<? extends IBaseBackboneElement> prefetchData;
        private IIdType additionalDataId;
        private IBaseParameters parameters;
        private List<? extends IBaseBackboneElement> library;
        private String cqlContent;

        public When(IRepository repository, CqlProcessor processor) {
            this.repository = repository;
            this.processor = processor;
            useServerData = true;
            jsonParser = repository.fhirContext().newJsonParser();
        }

        public When subject(String id) {
            subject = id;
            return this;
        }

        public When expression(String value) {
            expression = value;
            return this;
        }

        public When useServerData(boolean value) {
            useServerData = value;
            return this;
        }

        public When cqlContent(String value) {
            cqlContent = value;
            return this;
        }

        public When data(String dataAssetName) {
            dataRepository = new InMemoryFhirRepository(
                    repository.fhirContext(), (IBaseBundle) jsonParser.parseResource(open(dataAssetName)));
            return this;
        }

        public When content(String dataAssetName) {
            contentRepository = new InMemoryFhirRepository(
                    repository.fhirContext(), (IBaseBundle) jsonParser.parseResource(open(dataAssetName)));
            return this;
        }

        public When terminology(String dataAssetName) {
            terminologyRepository = new InMemoryFhirRepository(
                    repository.fhirContext(), (IBaseBundle) jsonParser.parseResource(open(dataAssetName)));
            return this;
        }

        private void loadAdditionalData(IBaseResource resource) {
            var fhirVersion = repository.fhirContext().getVersion().getVersion();
            additionalData = resource.getIdElement().getResourceType().equals("Bundle")
                    ? (IBaseBundle) resource
                    : addEntry(newBundle(fhirVersion), newEntryWithResource(resource));
        }

        public When additionalData(String dataAssetName) {
            var data = jsonParser.parseResource(open(dataAssetName));
            loadAdditionalData(data);
            return this;
        }

        public When additionalDataId(IIdType id) {
            additionalDataId = id;
            return this;
        }

        public When prefetchData(List<IBaseBackboneElement> prefetchData) {
            this.prefetchData = prefetchData;
            return this;
        }

        public When prefetchData(String name, String dataAssetName) {
            var data = jsonParser.parseResource(open(dataAssetName));
            prefetchData = List.of((IBaseBackboneElement) newPart(repository.fhirContext(), name, data));
            return this;
        }

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public Evaluation thenEvaluate() {
            if (additionalDataId != null) {
                loadAdditionalData(readRepository(repository, additionalDataId));
            }
            return new Evaluation(
                    repository,
                    processor.evaluate(
                            subject,
                            expression,
                            parameters,
                            library,
                            useServerData,
                            additionalData,
                            prefetchData,
                            cqlContent,
                            dataRepository,
                            contentRepository,
                            terminologyRepository));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class Evaluation {
        final IRepository repository;
        final IBaseParameters result;
        final IParser jsonParser;
        final ModelResolver modelResolver;
        final List<IBaseResource> parameter;

        @SuppressWarnings("unchecked")
        public Evaluation(IRepository repository, IBaseParameters result) {
            this.repository = repository;
            this.result = result;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
            parameter = ((List<IBaseResource>) modelResolver.resolvePath(result, "parameter"));
        }

        public Evaluation hasResults(Integer count) {
            assertEquals(count, parameter.size());
            return this;
        }

        public Evaluation hasOperationOutcome() {
            assertTrue(modelResolver.resolvePath(parameter.get(0), "resource") instanceof IBaseOperationOutcome);
            return this;
        }

        public Evaluation resultHasValue(Integer index, IBase value) {
            var actual = parameter.get(index);
            assertEquals(value, actual);
            return this;
        }
    }
}
