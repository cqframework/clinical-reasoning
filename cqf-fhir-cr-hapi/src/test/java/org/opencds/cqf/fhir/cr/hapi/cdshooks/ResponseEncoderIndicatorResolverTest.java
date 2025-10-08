package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceIndicatorEnum;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceResponseEncoder.ResponseEncoderIndicatorResolver;

class ResponseEncoderIndicatorResolverTest {

    @Test
    void testResolveIndicator_routineCode() {
        ResponseEncoderIndicatorResolver resolver = new ResponseEncoderIndicatorResolver();
        CdsServiceIndicatorEnum result = resolver.resolveIndicator("routine");
        assertEquals(CdsServiceIndicatorEnum.INFO, result, "Expected INFO indicator for routine code");
    }

    @Test
    void testResolveIndicator_urgentCode() {
        ResponseEncoderIndicatorResolver resolver = new ResponseEncoderIndicatorResolver();
        CdsServiceIndicatorEnum result = resolver.resolveIndicator("urgent");
        assertEquals(CdsServiceIndicatorEnum.WARNING, result, "Expected WARNING indicator for urgent code");
    }

    @Test
    void testResolveIndicator_statCode() {
        ResponseEncoderIndicatorResolver resolver = new ResponseEncoderIndicatorResolver();
        CdsServiceIndicatorEnum result = resolver.resolveIndicator("stat");
        assertEquals(CdsServiceIndicatorEnum.CRITICAL, result, "Expected CRITICAL indicator for stat code");
    }

    @Test
    void testResolveIndicator_invalidCode() {
        ResponseEncoderIndicatorResolver resolver = new ResponseEncoderIndicatorResolver();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveIndicator("invalid"),
                "Expected IllegalArgumentException for invalid code"
        );
        assertTrue(exception.getMessage().contains("Invalid priority code: invalid"));
    }

    @Test
    void testResolveIndicator_nullCode() {
        ResponseEncoderIndicatorResolver resolver = new ResponseEncoderIndicatorResolver();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveIndicator(null),
                "Expected IllegalArgumentException for null code"
        );
        assertTrue(exception.getMessage().contains("Invalid priority code: null"));
    }
}