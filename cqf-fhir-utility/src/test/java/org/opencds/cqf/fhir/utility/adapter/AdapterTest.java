package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

class AdapterTest {
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
        assertNotNull(adapter.getModelResolver());
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
}
