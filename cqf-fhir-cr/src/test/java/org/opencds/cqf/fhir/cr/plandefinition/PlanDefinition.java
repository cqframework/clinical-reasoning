package org.opencds.cqf.fhir.cr.plandefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;
import static org.opencds.cqf.fhir.utility.SearchHelper.readRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.skyscreamer.jsonassert.JSONAssert;

public class PlanDefinition {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/plandefinition";

    private static final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

    static {
        evaluationSettings
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

        evaluationSettings
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
    }

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

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = TestRepositoryFactory.createRepository(
                    fhirContext, this.getClass(), CLASS_PATH + "/" + repositoryPath, IGLayoutMode.TYPE_PREFIX);
            return this;
        }

        public static PlanDefinitionProcessor buildProcessor(Repository repository) {
            return new PlanDefinitionProcessor(repository, evaluationSettings);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    public static class When {
        private final Repository repository;
        private final PlanDefinitionProcessor processor;
        private final IParser jsonParser;

        private String planDefinitionId;

        private String subjectId;
        private String encounterId;
        private String practitionerId;

        private Repository dataRepository;
        private Repository contentRepository;
        private Repository terminologyRepository;
        private IBaseBundle additionalData;
        private IIdType additionalDataId;
        private IBaseParameters parameters;

        public When(Repository repository, PlanDefinitionProcessor processor) {
            this.repository = repository;
            this.processor = processor;
            jsonParser = repository.fhirContext().newJsonParser();
        }

        public When planDefinitionId(String id) {
            planDefinitionId = id;
            return this;
        }

        public When subjectId(String id) {
            subjectId = id;
            return this;
        }

        public When encounterId(String id) {
            encounterId = id;
            return this;
        }

        public When practitionerId(String id) {
            practitionerId = id;
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

        private Repository buildRepository() {
            if (dataRepository == null && contentRepository == null && terminologyRepository == null) {
                return repository;
            }

            if (dataRepository == null) {
                dataRepository = repository;
            }
            if (contentRepository == null) {
                contentRepository = repository;
            }
            if (terminologyRepository == null) {
                terminologyRepository = repository;
            }
            return Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
        }

        public GeneratedBundle thenApplyR5() {
            var requestRepository = buildRepository();
            var libraryEngine = new LibraryEngine(requestRepository, processor.evaluationSettings);
            if (additionalDataId != null) {
                loadAdditionalData(readRepository(repository, additionalDataId));
            }
            return new GeneratedBundle(
                    repository,
                    processor.applyR5(
                            Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", planDefinitionId)),
                            subjectId,
                            encounterId,
                            practitionerId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            parameters,
                            null,
                            additionalData,
                            null,
                            libraryEngine));
        }

        public GeneratedCarePlan thenApply() {
            var requestRepository = buildRepository();
            var libraryEngine = new LibraryEngine(requestRepository, processor.evaluationSettings);
            if (additionalDataId != null) {
                loadAdditionalData(readRepository(repository, additionalDataId));
            }
            return new GeneratedCarePlan(
                    repository,
                    processor.apply(
                            Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", planDefinitionId)),
                            subjectId,
                            encounterId,
                            practitionerId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            parameters,
                            null,
                            additionalData,
                            null,
                            libraryEngine));
        }

        public GeneratedPackage thenPackage() {
            return new GeneratedPackage(processor.packagePlanDefinition(
                    Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", planDefinitionId)), true));
        }
    }

    public static class GeneratedBundle {
        final Repository repository;
        final IBaseBundle generatedBundle;
        final IParser jsonParser;
        final ModelResolver modelResolver;

        public GeneratedBundle(Repository repository, IBaseBundle generatedBundle) {
            this.repository = repository;
            this.generatedBundle = generatedBundle;
            jsonParser = this.repository.fhirContext().newJsonParser();
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
        }

        public void isEqualsTo(String expectedBundleAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedBundleAssetName), jsonParser.encodeResourceToString(generatedBundle), true);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void isEqualsTo(IIdType expectedBundleId) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(readRepository(repository, expectedBundleId)),
                        jsonParser.encodeResourceToString(generatedBundle),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void hasEntry(int count) {
            assertEquals(count, getEntry(generatedBundle).size());
        }

        public void hasCommunicationRequestPayload() {
            assertTrue(getEntryResources(generatedBundle).stream()
                    .filter(r -> r.fhirType().equals("CommunicationRequest"))
                    .allMatch(c -> modelResolver.resolvePath(c, "payload") != null));
        }

        @SuppressWarnings("unchecked")
        public void hasQuestionnaireOperationOutcome() {
            assertTrue(getEntryResources(generatedBundle).stream()
                    .anyMatch(r -> r.fhirType().equals("Questionnaire")
                            && ((List<IBaseResource>) modelResolver.resolvePath(r, "contained"))
                                    .stream().anyMatch(c -> c.fhirType().equals("OperationOutcome"))));
        }
    }

    public static class GeneratedCarePlan {
        final Repository repository;
        final IBaseResource generatedCarePlan;
        final IParser jsonParser;
        final ModelResolver modelResolver;

        public GeneratedCarePlan(Repository repository, IBaseResource generatedCarePlan) {
            this.repository = repository;
            this.generatedCarePlan = generatedCarePlan;
            jsonParser = this.repository.fhirContext().newJsonParser();
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
        }

        public void isEqualsTo(String expectedCarePlanAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedCarePlanAssetName), jsonParser.encodeResourceToString(generatedCarePlan), true);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void isEqualsTo(IIdType expectedCarePlanId) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(readRepository(repository, expectedCarePlanId)),
                        jsonParser.encodeResourceToString(generatedCarePlan),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        @SuppressWarnings("unchecked")
        public void hasContained(int count) {
            assertEquals(
                    count, ((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlan, "contained")).size());
        }

        @SuppressWarnings("unchecked")
        public void hasOperationOutcome() {
            assertTrue(((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlan, "contained"))
                    .stream().anyMatch(r -> r.fhirType().equals("OperationOutcome")));
        }
    }

    public static class GeneratedPackage {
        IBaseBundle generatedBundle;

        public GeneratedPackage(IBaseBundle generatedBundle) {
            this.generatedBundle = generatedBundle;
        }

        public void hasEntry(int count) {
            assertEquals(count, getEntry(generatedBundle).size());
        }
    }
}
