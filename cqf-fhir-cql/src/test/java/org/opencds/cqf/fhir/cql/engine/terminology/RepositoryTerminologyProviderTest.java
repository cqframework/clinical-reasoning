package org.opencds.cqf.fhir.cql.engine.terminology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;

class RepositoryTerminologyProviderTest {

    private static final String SYSTEM_FOR_CODES = "http://example.com/CodeSystem/Codes";
    private static final String VALID_VALUE_SET_URL = "http://example.com/ValueSet/ValidValueSet";
    private static final String MISSING_SYSTEM_VALUE_SET_URL = "http://example.com/ValueSet/MissingSystemValueSet";
    private static final String MISSING_CODE_VALUE_SET_URL = "http://example.com/ValueSet/MissingCodeValueSet";

    @Test
    void validCodesInValueSet() {
        var terminologyProvider = terminologyProviderWith("ValidValueSet");
        var vsInfo = new ValueSetInfo().withId(VALID_VALUE_SET_URL);

        var codeExistsInValueSet = new Code().withCode("123").withSystem(SYSTEM_FOR_CODES);
        assertTrue(terminologyProvider.in(codeExistsInValueSet, vsInfo));

        var codeDoesNotExistInValueSet = new Code().withCode("DNE").withSystem(SYSTEM_FOR_CODES);
        assertFalse(terminologyProvider.in(codeDoesNotExistInValueSet, vsInfo));
    }

    @Test
    void missingCodeValueSetMatches() {
        var terminologyProvider = terminologyProviderWith("MissingCodeValueSet");
        var vsInfo = new ValueSetInfo().withId(MISSING_CODE_VALUE_SET_URL);

        var validCode = new Code().withCode("123").withSystem(SYSTEM_FOR_CODES);
        assertTrue(terminologyProvider.in(validCode, vsInfo));

        var codeMissingCode = new Code().withSystem(SYSTEM_FOR_CODES);
        assertFalse(terminologyProvider.in(codeMissingCode, vsInfo));

        var codeMissingSystem = new Code().withCode("123");
        assertFalse(terminologyProvider.in(codeMissingSystem, vsInfo));
    }

    @Test
    void missingSystemValueSetMatches() {
        var terminologyProvider = terminologyProviderWith("MissingSystemValueSet");
        var vsInfo = new ValueSetInfo().withId(MISSING_SYSTEM_VALUE_SET_URL);

        var validCode = new Code().withCode("123").withSystem(SYSTEM_FOR_CODES);
        assertTrue(terminologyProvider.in(validCode, vsInfo));

        var codeMissingCode = new Code().withSystem(SYSTEM_FOR_CODES);
        assertFalse(terminologyProvider.in(codeMissingCode, vsInfo));

        var codeMissingSystem = new Code().withCode("123");
        assertFalse(terminologyProvider.in(codeMissingSystem, vsInfo));
    }

    IRepository mockRepositoryFor(String id) {
        var vs = loadValueSet(id);
        return mockRepositoryWithValueSet(vs);
    }

    IRepository mockRepositoryWithValueSet(ValueSet valueSet) {
        var mockRepository = mock(IRepository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        Bundle bundle = new Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Map.class), isNull())).thenReturn(bundle);
        return mockRepository;
    }

    ValueSet loadValueSet(String id) {
        var resourceStream = this.getClass().getResourceAsStream(id + ".json");
        return (ValueSet) FhirContext.forR4Cached().newJsonParser().parseResource(resourceStream);
    }

    TerminologyProvider terminologyProviderWith(String valueSetId) {
        var repository = mockRepositoryFor(valueSetId);
        return new RepositoryTerminologyProvider(repository, new TerminologySettings());
    }
}
