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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestItemGenerator {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

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
                    fhirContext, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
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

        public GeneratedItem then() {
            IBaseResource result;
            if (profileUrl != null || profileId != null || profile != null) {
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
        final Map<String, IBaseBackboneElement> items;

        public GeneratedItem(Repository repository, IBaseResource questionnaire) {
            this.repository = repository;
            this.questionnaire = questionnaire;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
            items = new HashMap<>();
            populateItems(getItems(questionnaire));
        }

        @SuppressWarnings("unchecked")
        private List<IBaseBackboneElement> getItems(IBase base) {
            var pathResult = modelResolver.resolvePath(base, "item");
            return pathResult instanceof List ? (List<IBaseBackboneElement>) pathResult : new ArrayList<>();
        }

        private void populateItems(List<IBaseBackboneElement> itemList) {
            for (var item : itemList) {
                @SuppressWarnings("unchecked")
                var linkIdPath = (IPrimitiveType<String>) modelResolver.resolvePath(item, "linkId");
                var linkId = linkIdPath == null ? null : linkIdPath.getValue();
                items.put(linkId, item);
                var childItems = getItems(item);
                if (!childItems.isEmpty()) {
                    populateItems(childItems);
                }
            }
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
            assertEquals(count, items.size());
            return this;
        }

        public GeneratedItem itemHasInitialValue(String linkId) {
            var item = items.get(linkId);
            assertNotNull(item);
            assertNotNull(modelResolver.resolvePath(item, "initial"));
            return this;
        }

        public GeneratedItem itemHasHiddenValueExtension(String linkId) {
            var item = items.get(linkId);
            assertNotNull(item);
            assertTrue(
                    item.getExtension().stream().anyMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_HIDDEN)));
            return this;
        }

        public GeneratedItem itemHasInitialExpression(String linkId) {
            var item = items.get(linkId);
            assertNotNull(item);
            assertTrue(item.getExtension().stream()
                    .anyMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)));
            return this;
        }

        public GeneratedItem itemHasExtractionValueExtension(String linkId) {
            var item = items.get(linkId);
            assertNotNull(item);
            assertTrue(item.getExtension().stream()
                    .anyMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE)));
            return this;
        }

        public GeneratedItem hasId(String expectedId) {
            assertEquals(expectedId, questionnaire.getIdElement().getIdPart());
            return this;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public GeneratedItem hasLaunchContextExtension(int count) {
            var pathResult = modelResolver.resolvePath(questionnaire, "extension");
            var exts = pathResult instanceof List ? (List<IBaseExtension>) pathResult : new ArrayList<>();
            var launchContextExts = exts.stream()
                    .filter(e -> {
                        var urlPathResult = modelResolver.resolvePath(e, "url");
                        if (urlPathResult instanceof IPrimitiveType) {
                            return ((IPrimitiveType<String>) urlPathResult)
                                    .getValueAsString()
                                    .equals(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
                        } else {
                            return false;
                        }
                    })
                    .toList();
            assertEquals(count, launchContextExts.size());
            return this;
        }
    }
}
