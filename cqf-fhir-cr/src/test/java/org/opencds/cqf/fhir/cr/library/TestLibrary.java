package org.opencds.cqf.fhir.cr.library;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;
import static org.opencds.cqf.fhir.utility.SearchHelper.readRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.TestOperationProvider;
import org.opencds.cqf.fhir.cr.helpers.GeneratedPackage;
import org.opencds.cqf.fhir.cr.plandefinition.PlanDefinition;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.Reflections;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ParametersAdapter;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class TestLibrary {
    // Borrowing resources from PlanDefinition
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/plandefinition";

    private static InputStream open(String asset) {
        return PlanDefinition.class.getResourceAsStream(asset);
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

    public static class Given {
        private Repository repository;
        private EvaluationSettings evaluationSettings;

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        public LibraryProcessor buildProcessor(Repository repository) {
            if (repository instanceof IgRepository) {
                ((IgRepository) repository)
                        .setOperationProvider(TestOperationProvider.newProvider(repository.fhirContext()));
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
            return new LibraryProcessor(repository, evaluationSettings);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    public static class When {
        private final Repository repository;
        private final LibraryProcessor processor;
        private final IParser jsonParser;

        private String libraryId;

        private String subject;
        private List<String> expression;
        private Boolean useServerData;
        private Repository dataRepository;
        private Repository contentRepository;
        private Repository terminologyRepository;
        private IBaseBundle additionalData;
        private IIdType additionalDataId;
        private IBaseParameters parameters;
        private Boolean isPackagePut;

        public When(Repository repository, LibraryProcessor processor) {
            this.repository = repository;
            this.processor = processor;
            jsonParser = repository.fhirContext().newJsonParser();
        }

        public When libraryId(String id) {
            libraryId = id;
            return this;
        }

        public When subject(String value) {
            subject = value;
            return this;
        }

        public When expression(List<String> value) {
            expression = value;
            return this;
        }

        public When useServerData(Boolean value) {
            useServerData = value;
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
                    : addEntry(newBundle(fhirVersion), newEntryWithResource(fhirVersion, resource));
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

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public When isPut(Boolean value) {
            isPackagePut = value;
            return this;
        }

        public GeneratedPackage thenPackage() {
            if (isPackagePut == null) {
                return new GeneratedPackage(
                        processor.packageLibrary(
                                Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "Library", libraryId))),
                        repository.fhirContext());
            } else {
                return new GeneratedPackage(
                        processor.packageLibrary(
                                Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "Library", libraryId)),
                                isPackagePut),
                        repository.fhirContext());
            }
        }

        public EvaluationResult thenEvaluate() {
            if (additionalDataId != null) {
                loadAdditionalData(readRepository(repository, additionalDataId));
            }
            return new EvaluationResult(processor.evaluate(
                    Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "Library", libraryId)),
                    subject,
                    expression,
                    parameters,
                    useServerData,
                    additionalData,
                    null,
                    dataRepository,
                    contentRepository,
                    terminologyRepository));
        }
    }

    public static class EvaluationResult {
        private final ParametersAdapter adapter;

        public EvaluationResult(IBaseParameters result) {
            adapter = AdapterFactory.forFhirVersion(result.getStructureFhirVersionEnum())
                    .createParameters(result);
        }

        protected IBase expressionResult(String expression) {
            return adapter.getParameter(expression);
        }

        public EvaluationResult hasErrors(boolean hasErrors) {
            var errorsExist = adapter.getParameter().stream().anyMatch(p -> {
                var resource = Reflections.getAccessor(p.getClass(), "resource")
                        .getFirstValueOrNull(p)
                        .orElse(null);
                return resource != null && resource.fhirType().equals("OperationOutcome");
            });
            assertEquals(hasErrors, errorsExist);
            return this;
        }

        public EvaluationResult hasExpression(String expression) {
            assertNotNull(expressionResult(expression));
            return this;
        }
    }
}
