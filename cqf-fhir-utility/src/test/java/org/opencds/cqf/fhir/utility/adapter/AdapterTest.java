package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.Assert.assertThrows;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;

import org.hl7.fhir.r4.model.Library;
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
    void testResolvPath() {
        var library = new Library();
        library.setDate(new Date());
        var adapter = IAdapterFactory.createAdapterForResource(library);
        assertThrows(UnprocessableEntityException.class, () -> adapter.resolvePathString(library,"date"));
    }
}
