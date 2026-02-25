package org.opencds.cqf.fhir.utility.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;

class DynamicModelResolverTest {

    private static final String TRANSLATION_EXT_URL = "http://hl7.org/fhir/StructureDefinition/translation";

    private DynamicModelResolver resolver;
    private FhirContext fhirContext;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4Cached();
        resolver = new DynamicModelResolver(new R4FhirModelResolver(), fhirContext);
    }

    @Test
    void setNestedValue_setsValueCodeOnTranslationExtension_whenCommunicationRequestHasNoExistingPayload() {
        var communicationRequest = new CommunicationRequest();
        RuntimeResourceDefinition def = fhirContext.getResourceDefinition(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode";
        var value = new StringType("en");

        resolver.setNestedValue(communicationRequest, path, value, def);

        assertNotNull(communicationRequest.getPayloadFirstRep());
        var contentString = communicationRequest.getPayloadFirstRep().getContent();
        assertNotNull(contentString, "contentString should have been created");

        var translationExt = ((StringType) contentString).getExtensionByUrl(TRANSLATION_EXT_URL);
        assertNotNull(translationExt, "translation extension should have been created");

        var langExt = translationExt.getExtensionByUrl("lang");
        assertNotNull(langExt, "lang extension should have been created");

        assertEquals("en", langExt.getValue().primitiveValue());
    }

    @Test
    void setNestedValue_setsValueCodeOnTranslationExtension_whenPayloadAlreadyExists() {
        var communicationRequest = new CommunicationRequest();
        communicationRequest.addPayload().setContent(new StringType("Hello"));

        RuntimeResourceDefinition def = fhirContext.getResourceDefinition(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode";
        var value = new StringType("en");

        resolver.setNestedValue(communicationRequest, path, value, def);

        var contentString =
                (StringType) communicationRequest.getPayloadFirstRep().getContent();
        assertNotNull(contentString);
        assertEquals("Hello", contentString.getValue());

        var translationExt = contentString.getExtensionByUrl(TRANSLATION_EXT_URL);
        assertNotNull(translationExt, "translation extension should have been created on existing contentString");

        var langExt = translationExt.getExtensionByUrl("lang");
        assertNotNull(langExt, "lang extension should have been created");

        assertEquals("en", langExt.getValue().primitiveValue());
    }

    @Test
    void setNestedValue_setsValueCodeOnTranslationExtension_whenTranslationExtensionAlreadyExists() {
        var communicationRequest = new CommunicationRequest();
        var contentString = new StringType("Hello");
        var translationExt = new Extension(TRANSLATION_EXT_URL);
        contentString.addExtension(translationExt);
        communicationRequest.addPayload().setContent(contentString);

        RuntimeResourceDefinition def = fhirContext.getResourceDefinition(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode";
        var value = new StringType("en");

        resolver.setNestedValue(communicationRequest, path, value, def);

        var resultTranslationExt = contentString.getExtensionByUrl(TRANSLATION_EXT_URL);
        assertNotNull(resultTranslationExt);

        var langExt = resultTranslationExt.getExtensionByUrl("lang");
        assertNotNull(langExt, "lang extension should have been created on existing translation extension");

        assertEquals("en", langExt.getValue().primitiveValue());
    }

    @Test
    void setNestedValue_setsValueStringOnTranslationExtension() {
        var communicationRequest = new CommunicationRequest();
        communicationRequest.addPayload().setContent(new StringType("Hello"));

        RuntimeResourceDefinition def = fhirContext.getResourceDefinition(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('content').valueString";
        var value = new StringType("Hej");

        resolver.setNestedValue(communicationRequest, path, value, def);

        var contentString =
                (StringType) communicationRequest.getPayloadFirstRep().getContent();
        var translationExt = contentString.getExtensionByUrl(TRANSLATION_EXT_URL);
        assertNotNull(translationExt);

        var contentExt = translationExt.getExtensionByUrl("content");
        assertNotNull(contentExt, "content extension should have been created");

        assertEquals("Hej", contentExt.getValue().primitiveValue());
    }

    @Test
    void setNestedValue_setsMultipleTranslationProperties() {
        var communicationRequest = new CommunicationRequest();
        communicationRequest.addPayload().setContent(new StringType("Hello"));

        RuntimeResourceDefinition def = fhirContext.getResourceDefinition(communicationRequest);

        // Set lang
        resolver.setNestedValue(
                communicationRequest,
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode",
                new StringType("da"),
                def);

        // Set content
        resolver.setNestedValue(
                communicationRequest,
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('content').valueString",
                new StringType("Hej"),
                def);

        var contentString =
                (StringType) communicationRequest.getPayloadFirstRep().getContent();
        var translationExt = contentString.getExtensionByUrl(TRANSLATION_EXT_URL);
        assertNotNull(translationExt);

        var langExt = translationExt.getExtensionByUrl("lang");
        assertNotNull(langExt);
        assertEquals("da", langExt.getValue().primitiveValue());

        var contentExt = translationExt.getExtensionByUrl("content");
        assertNotNull(contentExt);
        assertEquals("Hej", contentExt.getValue().primitiveValue());
    }

    @Test
    void setNestedValue_setsSecondTranslation() {
        var communicationRequest = new CommunicationRequest();
        var contentString = new StringType("Hello");
        var existingTranslation = new Extension(TRANSLATION_EXT_URL);
        existingTranslation.addExtension("lang", new StringType("da"));
        existingTranslation.addExtension("content", new StringType("Hej"));
        contentString.addExtension(existingTranslation);
        communicationRequest.addPayload().setContent(contentString);

        RuntimeResourceDefinition def = fhirContext.getResourceDefinition(communicationRequest);

        // Add a second translation at index [1]
        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[1].extension('lang').valueCode";
        var value = new StringType("de");

        resolver.setNestedValue(communicationRequest, path, value, def);

        var extensions = contentString.getExtensionsByUrl(TRANSLATION_EXT_URL);
        assertEquals(2, extensions.size(), "Should have two translation extensions");

        var secondTranslation = extensions.get(1);
        var langExt = secondTranslation.getExtensionByUrl("lang");
        assertNotNull(langExt);
        assertEquals("de", langExt.getValue().primitiveValue());
    }
}
