package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
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
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestItemGenerator {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/questionnaire";

    private static InputStream open(String asset) {
        return TestItemGenerator.class.getResourceAsStream(asset);
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

        public static QuestionnaireProcessor buildProcessor(Repository repository) {
            return new QuestionnaireProcessor(repository);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    public static class When {
        private final Repository repository;
        private final QuestionnaireProcessor processor;
        private IIdType profileId;
        private IPrimitiveType<String> profileUrl;
        private IBaseResource profile;
        private String id;
        private String subjectId;
        private IBaseBundle data;
        private IBaseParameters parameters;

        When(Repository repository, QuestionnaireProcessor itemGenerator) {
            this.repository = repository;
            this.processor = itemGenerator;
        }

        public When profileId(IIdType id) {
            profileId = id;
            return this;
        }

        public When profileUrl(IPrimitiveType<String> url) {
            profileUrl = url;
            return this;
        }

        public When profile(IBaseResource resource) {
            profile = resource;
            return this;
        }

        public When id(String id) {
            this.id = id;
            return this;
        }

        public When subjectId(String id) {
            subjectId = id;
            return this;
        }

        public When bundle(IBaseBundle bundle) {
            this.data = bundle;
            return this;
        }

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public GeneratedItem then() {
            IBaseResource result;
            if (subjectId != null || parameters != null || data != null) {
                result = processor.generateQuestionnaire(
                        Eithers.for3(profileUrl, profileId, profile),
                        false,
                        true,
                        subjectId,
                        parameters,
                        true,
                        data,
                        null,
                        id);
            } else if (profileUrl != null || profileId != null || profile != null) {
                result = processor.generateQuestionnaire(Eithers.for3(profileUrl, profileId, profile), false, true);
            } else {
                result = processor.generateQuestionnaire(id);
            }
            return new GeneratedItem(repository, result);
        }
    }

    public static class GeneratedItem {
        final Repository repository;
        final IBaseResource questionnaire;
        final IParser jsonParser;
        final ModelResolver modelResolver;

        public GeneratedItem(Repository repository, IBaseResource questionnaire) {
            this.repository = repository;
            this.questionnaire = questionnaire;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
        }

        public void isEqualsTo(String expectedItemAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedItemAssetName), jsonParser.encodeResourceToString(questionnaire), true);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void isEqualsTo(IIdType expectedQuestionnaireId) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(readRepository(repository, expectedQuestionnaireId)),
                        jsonParser.encodeResourceToString(questionnaire),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public GeneratedItem hasItemCount(int count) {
            var item = ((List<IBase>) modelResolver.resolvePath(questionnaire, "item")).get(0);
            assertNotNull(item);
            var childItem = (List<IBase>) modelResolver.resolvePath(item, "item");
            assertEquals(count, childItem.size());
            return this;
        }

        public GeneratedItem itemHasInitialValue() {
            var item = ((List<IBase>) modelResolver.resolvePath(questionnaire, "item")).get(0);
            var childItem = (List<IBase>) modelResolver.resolvePath(item, "item");
            assertTrue(childItem.stream().anyMatch(i -> modelResolver.resolvePath(i, "initial") != null));
            return this;
        }

        public GeneratedItem hasId(String expectedId) {
            assertEquals(expectedId, questionnaire.getIdElement().getIdPart());
            return this;
        }
    }
}
