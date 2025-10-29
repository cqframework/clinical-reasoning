package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.IExtractProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestQuestionnaireResponse {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

    public static InputStream open(String asset) {
        var path = Path.of(getResourcePath(TestQuestionnaireResponse.class) + "/" + CLASS_PATH + "/" + asset);
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
        private final List<IOperationProcessor> operationProcessors = new ArrayList<>();

        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public Given extractProcessor(IExtractProcessor extractProcessor) {
            operationProcessors.add(extractProcessor);
            return this;
        }

        private QuestionnaireResponseProcessor buildProcessor() {
            return new QuestionnaireResponseProcessor(repository, CrSettings.getDefault(), operationProcessors);
        }

        public When when() {
            return new When(buildProcessor());
        }
    }

    public static class When {
        private final QuestionnaireResponseProcessor processor;
        private String questionnaireResponseId;
        private IBaseResource questionnaireResponse;
        private String questionnaireId;
        private IBaseResource questionnaire;

        public When(QuestionnaireResponseProcessor processor) {
            this.processor = processor;
        }

        public When questionnaireResponseId(String questionnaireResponseId) {
            this.questionnaireResponseId = questionnaireResponseId;
            return this;
        }

        public When questionnaireResponse(IBaseResource questionnaireResponse) {
            this.questionnaireResponse = questionnaireResponse;
            return this;
        }

        public When questionnaireId(String questionnaireId) {
            this.questionnaireId = questionnaireId;
            return this;
        }

        public When questionnaire(IBaseResource questionnaire) {
            this.questionnaire = questionnaire;
            return this;
        }

        public Extract extract() {
            return new Extract(
                    processor.repository,
                    processor.extract(
                            Eithers.for2(
                                    questionnaireResponseId == null
                                            ? null
                                            : Ids.newId(
                                                    processor.fhirVersion,
                                                    "QuestionnaireResponse",
                                                    questionnaireResponseId),
                                    questionnaireResponse),
                            questionnaire == null && questionnaireId == null
                                    ? null
                                    : Eithers.for2(
                                            questionnaireId == null
                                                    ? null
                                                    : Ids.newId(
                                                            processor.fhirVersion, "Questionnaire", questionnaireId),
                                            questionnaire),
                            null,
                            null,
                            true));
        }
    }

    public static class Extract {
        private final IRepository repository;
        private final IBaseBundle bundle;
        private final IParser jsonParser;
        private final ModelResolver modelResolver;

        public Extract(IRepository repository, IBaseBundle bundle) {
            this.repository = repository;
            this.bundle = bundle;
            jsonParser = repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    repository.fhirContext().getVersion().getVersion());
        }

        public Extract isEqualsTo(String expectedBundleAssetName) {
            assertDoesNotThrow(
                    () -> {
                        JSONAssert.assertEquals(
                                load(expectedBundleAssetName), jsonParser.encodeResourceToString(bundle), true);
                    },
                    "Unable to compare Jsons: ");
            return this;
        }

        public Extract isEqualsToExpected(IIdType expectedBundleId) {
            var expectedBundle = repository.read(bundle.getClass(), expectedBundleId);
            assertDoesNotThrow(
                    () -> {
                        JSONAssert.assertEquals(
                                jsonParser.encodeResourceToString(expectedBundle),
                                jsonParser.encodeResourceToString(bundle),
                                true);
                    },
                    "Unable to compare Jsons: ");
            return this;
        }

        public Extract hasEntry(int count) {
            var entry = (List<?>) modelResolver.resolvePath(bundle, "entry");
            assertEquals(count, entry.size());
            return this;
        }

        public IBaseBundle getBundle() {
            return bundle;
        }
    }
}
