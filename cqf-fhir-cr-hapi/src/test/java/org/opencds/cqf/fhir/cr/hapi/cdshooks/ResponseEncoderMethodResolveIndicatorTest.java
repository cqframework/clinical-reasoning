package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceIndicatorEnum;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("UnstableApiUsage")
class ResponseEncoderMethodResolveIndicatorTest {

    record ProvideResolveIndicatorTestCasesParams(String indicator, CdsServiceIndicatorEnum expectedIndicator) {}

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    private CdsResponseEncoderService fixture;

    @BeforeEach
    void beforeEach() {
        IRepository repository = new InMemoryFhirRepository(fhirContext);
        fixture = new CdsResponseEncoderService(repository);
    }

    @ParameterizedTest
    @MethodSource("provideResolveIndicatorTestCases")
    void testResolveIndicator_routineCode(ProvideResolveIndicatorTestCasesParams params) {
        CdsServiceIndicatorEnum result = fixture.resolveIndicator(params.indicator());
        assertEquals(params.expectedIndicator(), result);
    }

    @Test
    void testResolveIndicator_invalidCode() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> fixture.resolveIndicator("invalid"));

        assertTrue(exception.getMessage().contains("Invalid priority code: invalid"));
    }

    private static Stream<ProvideResolveIndicatorTestCasesParams> provideResolveIndicatorTestCases() {
        return Stream.of(
                new ProvideResolveIndicatorTestCasesParams("routine", CdsServiceIndicatorEnum.INFO),
                new ProvideResolveIndicatorTestCasesParams("urgent", CdsServiceIndicatorEnum.WARNING),
                new ProvideResolveIndicatorTestCasesParams("stat", CdsServiceIndicatorEnum.CRITICAL));
    }
}
