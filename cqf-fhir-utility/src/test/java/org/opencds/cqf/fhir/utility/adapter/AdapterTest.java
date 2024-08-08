package org.opencds.cqf.fhir.utility.adapter;

import static org.junit.Assert.assertThrows;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import org.junit.jupiter.api.Test;

class AdapterTest {
    @Test
    void testUnsupportedVersion() {
        var version = FhirVersionEnum.DSTU2;
        assertThrows(UnprocessableEntityException.class, () -> Adapter.newPeriod(version));
        assertThrows(UnprocessableEntityException.class, () -> Adapter.newStringType(version, "string"));
        assertThrows(UnprocessableEntityException.class, () -> Adapter.newUriType(version, "uri"));
        assertThrows(UnprocessableEntityException.class, () -> Adapter.newUrlType(version, "url"));
        assertThrows(UnprocessableEntityException.class, () -> Adapter.newDateType(version, new Date()));
        assertThrows(UnprocessableEntityException.class, () -> Adapter.newDateTimeType(version, new Date()));
    }
}
