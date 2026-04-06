package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

class AdapterTest {
    private static final String TRANSLATION_EXT_URL = "http://hl7.org/fhir/StructureDefinition/translation";

    @Test
    void testUnsupportedVersion() {
        var version = FhirVersionEnum.DSTU2;
        assertThrows(UnprocessableEntityException.class, () -> IAdapter.newPeriod(version));
        assertThrows(UnprocessableEntityException.class, () -> IAdapter.newStringType(version, "string"));
        assertThrows(UnprocessableEntityException.class, () -> IAdapter.newUriType(version, "uri"));
        assertThrows(UnprocessableEntityException.class, () -> IAdapter.newUrlType(version, "url"));
        var date = new Date();
        assertThrows(UnprocessableEntityException.class, () -> IAdapter.newDateType(version, date));
        assertThrows(UnprocessableEntityException.class, () -> IAdapter.newDateTimeType(version, date));
    }

    @Test
    void testResolvePath() {
        var library = new Library();
        library.setDate(new Date());
        var adapter = IAdapterFactory.createAdapterForResource(library);
        assertThrows(UnprocessableEntityException.class, () -> adapter.resolvePathString(library, "date"));
    }

    @Test
    void testBaseAdapter() {
        var fhirVersion = FhirVersionEnum.R4;
        var coding = new Coding();
        var factory = IAdapterFactory.forFhirVersion(fhirVersion);
        assertThrows(IllegalArgumentException.class, () -> factory.createCoding(null));
        var adapter = factory.createCoding(coding);
        assertEquals(coding, adapter.get());
        assertEquals(fhirVersion, adapter.fhirVersion());
        assertNotNull(adapter.getAdapterFactory());
    }

    @Test
    void testBaseResourceAdapter() {
        var extUrl = "test.com";
        var extValue = new StringType("test");
        var resource = new Observation();
        var adapter = IAdapterFactory.createAdapterForResource(resource);
        assertFalse(adapter.hasExtension());
        var newExtension = adapter.addExtension();
        newExtension.setUrl(extUrl);
        newExtension.setValue(extValue);
        assertTrue(adapter.hasExtension());
        assertEquals(extValue, adapter.getExtensionByUrl(extUrl).getValue());
    }

    @Test
    void setNestedValue_setsValueCodeOnTranslationExtension_whenCommunicationRequestHasNoExistingPayload() {
        var communicationRequest = new CommunicationRequest();
        var adapter = IAdapterFactory.createAdapterForResource(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode";
        var value = new StringType("en");

        adapter.setValue(communicationRequest, path, value);

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
        var adapter = IAdapterFactory.createAdapterForResource(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode";
        var value = new StringType("en");

        adapter.setValue(communicationRequest, path, value);

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
        var adapter = IAdapterFactory.createAdapterForResource(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode";
        var value = new StringType("en");

        adapter.setValue(communicationRequest, path, value);

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
        var adapter = IAdapterFactory.createAdapterForResource(communicationRequest);

        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('content').valueString";
        var value = new StringType("Hej");

        adapter.setValue(communicationRequest, path, value);

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
        var adapter = IAdapterFactory.createAdapterForResource(communicationRequest);

        // Set lang
        adapter.setValue(
                communicationRequest,
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('lang').valueCode",
                new StringType("da"));

        // Set content
        adapter.setValue(
                communicationRequest,
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[0].extension('content').valueString",
                new StringType("Hej"));

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
        var adapter = IAdapterFactory.createAdapterForResource(communicationRequest);

        // Add a second translation at index [1]
        var path =
                "payload[0].contentString.extension('http://hl7.org/fhir/StructureDefinition/translation')[1].extension('lang').valueCode";
        var value = new StringType("de");

        adapter.setValue(communicationRequest, path, value);

        var extensions = contentString.getExtensionsByUrl(TRANSLATION_EXT_URL);
        assertEquals(2, extensions.size(), "Should have two translation extensions");

        var secondTranslation = extensions.get(1);
        var langExt = secondTranslation.getExtensionByUrl("lang");
        assertNotNull(langExt);
        assertEquals("de", langExt.getValue().primitiveValue());
    }
}
