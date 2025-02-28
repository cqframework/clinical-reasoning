package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.IExtractProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestQuestionnaireResponse {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

    private TestQuestionnaireResponse() {
        // private constructor
    }

    public static InputStream open(String asset) {
        var path = Paths.get(
                String.format("%s/%s/%s", getResourcePath(TestQuestionnaireResponse.class), CLASS_PATH, asset));
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
        private Repository repository;
        private IExtractProcessor extractProcessor;

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext,
                    Paths.get(String.format("%s/%s/%s", getResourcePath(this.getClass()), CLASS_PATH, repositoryPath)));
            return this;
        }

        public Given extractProcessor(IExtractProcessor extractProcessor) {
            this.extractProcessor = extractProcessor;
            return this;
        }

        private QuestionnaireResponseProcessor buildProcessor() {
            return new QuestionnaireResponseProcessor(repository, EvaluationSettings.getDefault(), extractProcessor);
        }

        public When when() {
            return new When(repository, buildProcessor());
        }
    }

    public static class When {
        private final Repository repository;
        private final QuestionnaireResponseProcessor processor;
        private final FhirVersionEnum fhirVersion;
        private String questionnaireResponseId;
        private IBaseResource questionnaireResponse;
        private String questionnaireId;
        private IBaseResource questionnaire;

        public When(Repository repository, QuestionnaireResponseProcessor processor) {
            this.repository = repository;
            this.processor = processor;
            fhirVersion = repository.fhirContext().getVersion().getVersion();
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
                    repository,
                    processor.extract(
                            Eithers.for2(
                                    questionnaireResponseId == null
                                            ? null
                                            : Ids.newId(fhirVersion, "QuestionnaireResponse", questionnaireResponseId),
                                    questionnaireResponse),
                            questionnaire == null && questionnaireId == null
                                    ? null
                                    : Eithers.for2(
                                            questionnaireId == null
                                                    ? null
                                                    : Ids.newId(fhirVersion, "Questionnaire", questionnaireId),
                                            questionnaire),
                            null,
                            null,
                            true));
        }
    }

    public static class Extract {
        private final Repository repository;
        private final IBaseBundle bundle;
        private final IParser jsonParser;
        private final ModelResolver modelResolver;

        public Extract(Repository repository, IBaseBundle bundle) {
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
