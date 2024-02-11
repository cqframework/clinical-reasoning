package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.json.JSONException;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.ResourceCategoryMode;
import org.opencds.cqf.fhir.utility.repository.ResourceFilenameMode;
import org.opencds.cqf.fhir.utility.repository.ResourceTypeMode;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestQuestionnaireResponse {
    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    private static final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

    private static InputStream open(String asset) {
        return TestQuestionnaireResponse.class.getResourceAsStream(asset);
    }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String load(String asset) throws IOException {
        return load(open(asset));
    }

    public static IBaseResource parse(String asset) {
        return jsonParser.parseResource(open(asset));
    }

    public static QuestionnaireResponseProcessor buildProcessor(Repository repository) {
        return new QuestionnaireResponseProcessor(repository);
    }

    /** Fluent interface starts here **/
    public static class Assert {
        public static Extract that(String questionnaireResponseName) {
            return new Extract(questionnaireResponseName);
        }

        public static Extract that(IdType theId) {
            return new Extract(theId);
        }
    }

    public static class Extract {
        private final IdType questionnaireResponseId;
        private Repository repository;
        private QuestionnaireResponse questionnaireResponse;
        private IdType expectedBundleId;

        private final FhirContext fhirContext = FhirContext.forR4Cached();

        public Extract(String questionnaireResponseName) {
            questionnaireResponseId = null;
            questionnaireResponse = (QuestionnaireResponse) parse(questionnaireResponseName);
        }

        public Extract(IdType theId) {
            questionnaireResponseId = theId;
            questionnaireResponse = null;
        }

        private void buildRepository() {
            if (repository == null) {
                repository = new IGFileStructureRepository(
                        this.fhirContext,
                        this.getClass()
                                        .getProtectionDomain()
                                        .getCodeSource()
                                        .getLocation()
                                        .getPath() + "org/opencds/cqf/fhir/cr/questionnaireresponse/r4",
                        ResourceTypeMode.FLAT,
                        ResourceCategoryMode.FLAT,
                        ResourceFilenameMode.ID_ONLY,
                        EncodingEnum.JSON);
            }

            if (questionnaireResponse == null) {
                questionnaireResponse = repository.read(QuestionnaireResponse.class, questionnaireResponseId);
            }
        }

        public Extract withRepository(Repository theRepository) {
            repository = theRepository;

            return this;
        }

        public Extract withExpectedBundleId(IdType theBundleId) {
            expectedBundleId = theBundleId;

            return this;
        }

        public GeneratedBundle extract() {
            buildRepository();
            Bundle expectedBundle = null;
            if (expectedBundleId != null) {
                try {
                    expectedBundle = repository.read(Bundle.class, expectedBundleId);
                } catch (Exception e) {
                }
            }
            var libraryEngine = new LibraryEngine(repository, evaluationSettings);
            return new GeneratedBundle(
                    (Bundle) buildProcessor(repository).extract(questionnaireResponse, null, null, libraryEngine),
                    expectedBundle);
        }
    }

    public static class GeneratedBundle {
        Bundle myGeneratedBundle;
        Bundle myExpectedBundle;

        public GeneratedBundle(Bundle theGeneratedBundle, Bundle theExpectedBundle) {
            myGeneratedBundle = theGeneratedBundle;
            myExpectedBundle = theExpectedBundle;
        }

        public void isEqualsTo(String expectedBundleAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedBundleAssetName), jsonParser.encodeResourceToString(myGeneratedBundle), true);
            } catch (JSONException | IOException e) {
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void isEqualsToExpected() {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(myExpectedBundle),
                        jsonParser.encodeResourceToString(myGeneratedBundle),
                        true);
            } catch (JSONException e) {
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void hasEntry(int theCount) {
            assertEquals(myGeneratedBundle.getEntry().size(), theCount);
        }
    }
}
