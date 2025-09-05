package org.opencds.cqf.fhir.cr.graphdefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;
import static org.opencds.cqf.fhir.utility.model.FhirModelResolverCache.resolverForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.TestOperationProvider;
import org.opencds.cqf.fhir.cr.graphdefintion.GraphDefinitionProcessor;
import org.opencds.cqf.fhir.cr.graphdefintion.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.graphdefintion.apply.ApplyRequestBuilder;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings("UnstableApiUsage")
public class TestGraphDefinition {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

    private static InputStream open(String asset) {
        var path = Path.of(getResourcePath(TestGraphDefinition.class) + "/" + CLASS_PATH + "/" + asset);
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

    public static class Given {
        private IRepository repository;
        private EvaluationSettings evaluationSettings;

        public Given repository(IRepository repository) {
            this.repository = repository;
            this.evaluationSettings = EvaluationSettings.getDefault();
            return this;
        }

        public Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public GraphDefinitionProcessor buildProcessor(IRepository repository) {
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

            return new GraphDefinitionProcessor(repository);
        }

        public When when() {
            return new When(repository, buildProcessor(repository), evaluationSettings);
        }
    }

    public static class When {
        private IRepository repository;
        private GraphDefinitionProcessor processor;
        private ApplyRequestBuilder applyRequestBuilder;
        private IParser jsonParser;

        public When(
                IRepository repository,
                GraphDefinitionProcessor graphDefinitionProcessor,
                EvaluationSettings evaluationSettings) {
            this.repository = repository;
            this.processor = graphDefinitionProcessor;
            this.applyRequestBuilder = new ApplyRequestBuilder(this.repository, evaluationSettings);
            this.jsonParser = repository.fhirContext().newJsonParser();
        }

        public When graphDefinitionId(String graphDefinitionId) {
            applyRequestBuilder.withGraphDefinitionId(new IdType("GraphDefinition", graphDefinitionId));
            return this;
        }

        public When subjectId(String patientId) {
            applyRequestBuilder.withSubject("Patient/" + patientId);
            return this;
        }

        public When practitionerId(String practitionerId) {
            applyRequestBuilder.withPractitioner("Practitioner/" + practitionerId);
            return this;
        }

        public When data(String dataAssetName) {
            applyRequestBuilder.withDataRepository(createRepository(dataAssetName));
            return this;
        }

        public When content(String dataAssetName) {
            applyRequestBuilder.withContentRepository(createRepository(dataAssetName));
            return this;
        }

        public When terminology(String dataAssetName) {
            applyRequestBuilder.withTerminologyRepository(createRepository(dataAssetName));
            return this;
        }

        public When additionalData(IBaseResource resource) {
            var fhirVersion = repository.fhirContext().getVersion().getVersion();
            IBaseBundle additionalData =
                    resource.getIdElement().getResourceType().equals("Bundle")
                            ? (IBaseBundle) resource
                            : addEntry(newBundle(fhirVersion), newEntryWithResource(resource));

            applyRequestBuilder.withData(additionalData);
            return this;
        }

        public When dataRepository(IgRepository theRepository) {
            applyRequestBuilder.withDataRepository(theRepository);
            return this;
        }

        public When additionalData(String dataAssetName) {
            var data = jsonParser.parseResource(open(dataAssetName));
            return additionalData(data);
        }

        public GeneratedBundle thenApply() {
            ApplyRequest applyRequest = applyRequestBuilder.buildApplyRequest();
            IBaseResource generatedBundle = this.processor.apply(applyRequest);

            return new GeneratedBundle(repository, generatedBundle);
        }

        private IRepository createRepository(String dataAssetName) {
            return new InMemoryFhirRepository(
                    repository.fhirContext(), (IBaseBundle) jsonParser.parseResource(open(dataAssetName)));
        }

        public static class GeneratedBundle {

            final IRepository repository;
            final IBaseResource generatedResource;
            final IParser jsonParser;
            final ModelResolver modelResolver;

            public GeneratedBundle(IRepository theRepository, IBaseResource generatedResource) {
                this.repository = theRepository;
                this.generatedResource = generatedResource;

                FhirContext fhirContext = this.repository.fhirContext();
                jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
                modelResolver = resolverForVersion(fhirContext.getVersion().getVersion());
            }

            public void responseIsBundle() {
                assertThat(this.generatedResource).isInstanceOf(IBaseBundle.class);
            }
        }
    }
}
