package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestQuestionnaireResponse {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/questionnaireresponse";

    private static InputStream open(String asset) {
        return TestQuestionnaireResponse.class.getResourceAsStream(asset);
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
            this.repository = new IgRepository(
                    fhirContext, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        private QuestionnaireResponseProcessor buildProcessor() {
            return new QuestionnaireResponseProcessor(repository);
        }

        public When when() {
            return new When(buildProcessor());
        }
    }

    public static class When {
        private final QuestionnaireResponseProcessor processor;
        private String questionnaireResponseId;
        private IBaseResource questionnaireResponse;

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

        public Extract extract() {
            return new Extract(
                    processor.repository,
                    processor.extract(Eithers.for2(
                            Ids.newId(processor.fhirVersion, "QuestionnaireResponse", questionnaireResponseId),
                            questionnaireResponse)));
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
            try {
                JSONAssert.assertEquals(load(expectedBundleAssetName), jsonParser.encodeResourceToString(bundle), true);
            } catch (JSONException | IOException e) {
                fail("Unable to compare Jsons: " + e.getMessage());
            }
            return this;
        }

        public Extract isEqualsToExpected(IIdType expectedBundleId) {
            var expectedBundle = repository.read(bundle.getClass(), expectedBundleId);
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(expectedBundle),
                        jsonParser.encodeResourceToString(bundle),
                        true);
            } catch (JSONException e) {
                fail("Unable to compare Jsons: " + e.getMessage());
            }
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
