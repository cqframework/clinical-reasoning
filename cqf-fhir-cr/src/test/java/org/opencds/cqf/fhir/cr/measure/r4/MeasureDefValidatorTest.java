package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;

import ca.uhn.fhir.repository.IRepository;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opencds.cqf.fhir.cr.measure.common.CompositeMeasureDefValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidationContext;
import org.opencds.cqf.fhir.cr.measure.common.ValidationIssue;
import org.opencds.cqf.fhir.cr.measure.common.ValidationResult;
import org.opencds.cqf.fhir.cr.measure.common.ValidationSeverity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MeasureDefValidatorTest {

    private static final String LIBRARY_URL = "http://example.com/Library/TestLibrary";
    private static final String VALUESET_URL = "http://example.com/ValueSet/TestValueSet";

    @Mock
    private IRepository repository;

    private Measure measure;

    @BeforeEach
    void setUp() {
        measure = new Measure();
        measure.setId("TestMeasure");
        measure.setUrl("http://example.com/Measure/TestMeasure");
        measure.setLibrary(List.of(new CanonicalType(LIBRARY_URL)));
    }

    @SuppressWarnings("unchecked")
    private void mockLibrarySearch(Bundle result) {
        lenient()
                .doReturn(result)
                .when(repository)
                .search(eq(Bundle.class), eq(Library.class), any(Multimap.class), isNull());
    }

    @SuppressWarnings("unchecked")
    private void mockValueSetSearch(Bundle result) {
        lenient()
                .doReturn(result)
                .when(repository)
                .search(eq(Bundle.class), eq(ValueSet.class), any(Multimap.class), isNull());
    }

    private Bundle bundleWith(org.hl7.fhir.r4.model.Resource resource) {
        var bundle = new Bundle();
        bundle.addEntry().setResource(resource);
        return bundle;
    }

    private Bundle emptyBundle() {
        return new Bundle();
    }

    // --- R4CqlLibraryValidator tests ---

    @Test
    void libraryValidator_noLibrary_producesError() {
        measure.setLibrary(List.of());
        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var result = new R4CqlLibraryValidator().validate(context);

        assertTrue(result.hasErrors());
        assertEquals(
                R4CqlLibraryValidator.LIBRARY_NOT_FOUND,
                result.getBlockingErrors().get(0).code());
    }

    @Test
    void libraryValidator_libraryExists_noErrors() {
        var library = new Library();
        library.setUrl(LIBRARY_URL);
        mockLibrarySearch(bundleWith(library));

        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var result = new R4CqlLibraryValidator().validate(context);

        assertFalse(result.hasErrors());
    }

    @Test
    void libraryValidator_libraryNotFound_producesError() {
        mockLibrarySearch(emptyBundle());

        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var result = new R4CqlLibraryValidator().validate(context);

        assertTrue(result.hasErrors());
        var error = result.getBlockingErrors().get(0);
        assertEquals(R4CqlLibraryValidator.LIBRARY_NOT_FOUND, error.code());
        assertTrue(error.description().contains(LIBRARY_URL));
        assertTrue(error.remediation().contains("Ensure the Library resource"));
    }

    // --- R4ValueSetAvailabilityValidator tests ---

    @Test
    void valueSetValidator_valueSetMissing_producesWarning() {
        var library = new Library();
        library.setUrl(LIBRARY_URL);
        var dataReq = new DataRequirement();
        dataReq.addCodeFilter().setValueSet(VALUESET_URL);
        library.addDataRequirement(dataReq);

        mockLibrarySearch(bundleWith(library));
        mockValueSetSearch(emptyBundle());

        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var result = new R4ValueSetAvailabilityValidator().validate(context);

        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        var warning = result.getIssues().get(0);
        assertEquals(R4ValueSetAvailabilityValidator.VALUESET_UNAVAILABLE, warning.code());
        assertEquals(ValidationSeverity.WARNING, warning.severity());
    }

    @Test
    void valueSetValidator_valueSetExists_noWarnings() {
        var library = new Library();
        library.setUrl(LIBRARY_URL);
        var dataReq = new DataRequirement();
        dataReq.addCodeFilter().setValueSet(VALUESET_URL);
        library.addDataRequirement(dataReq);

        mockLibrarySearch(bundleWith(library));
        mockValueSetSearch(bundleWith(new ValueSet().setUrl(VALUESET_URL)));

        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var result = new R4ValueSetAvailabilityValidator().validate(context);

        assertTrue(result.isEmpty());
    }

    // --- R4ParameterConfigurationValidator tests ---

    @Test
    void parameterValidator_missingRequiredParam_producesError() {
        var library = new Library();
        library.setUrl(LIBRARY_URL);
        var paramDef = new ParameterDefinition();
        paramDef.setName("requiredParam");
        paramDef.setUse(ParameterDefinition.ParameterUse.IN);
        paramDef.setMin(1);
        paramDef.setType("String");
        library.addParameter(paramDef);

        mockLibrarySearch(bundleWith(library));

        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository, Map.of());

        var result = new R4ParameterConfigurationValidator().validate(context);

        assertTrue(result.hasErrors());
        assertEquals(
                "MISSING_REQUIRED_PARAMETER", result.getBlockingErrors().get(0).code());
    }

    @Test
    void parameterValidator_unknownParam_producesWarning() {
        var library = new Library();
        library.setUrl(LIBRARY_URL);
        var paramDef = new ParameterDefinition();
        paramDef.setName("knownParam");
        paramDef.setUse(ParameterDefinition.ParameterUse.IN);
        paramDef.setMin(0);
        library.addParameter(paramDef);

        mockLibrarySearch(bundleWith(library));

        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository, Map.of("unknownParam", "value"));

        var result = new R4ParameterConfigurationValidator().validate(context);

        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertEquals("UNKNOWN_PARAMETER", result.getIssues().get(0).code());
    }

    // --- R4ExpressionReferenceValidator tests ---

    @Test
    void expressionValidator_expressionNotFound_producesWarning() {
        var elmJson = """
                {
                    "library": {
                        "statements": {
                            "def": [
                                { "name": "Patient", "context": "Patient" },
                                { "name": "Initial Population" }
                            ]
                        }
                    }
                }
                """;

        var library = new Library();
        library.setUrl(LIBRARY_URL);
        library.addContent(
                new Attachment().setContentType("application/elm+json").setData(elmJson.getBytes()));

        mockLibrarySearch(bundleWith(library));

        var measureDef = new R4MeasureDefBuilder().build(createMeasureWithPopulation("Missing Expression"));
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var result = new R4ExpressionReferenceValidator().validate(context);

        assertTrue(result.hasWarnings());
        assertEquals(
                R4ExpressionReferenceValidator.EXPRESSION_NOT_FOUND,
                result.getIssues().get(0).code());
    }

    @Test
    void expressionValidator_expressionExists_noWarnings() {
        var elmJson = """
                {
                    "library": {
                        "statements": {
                            "def": [
                                { "name": "Patient", "context": "Patient" },
                                { "name": "Initial Population" }
                            ]
                        }
                    }
                }
                """;

        var library = new Library();
        library.setUrl(LIBRARY_URL);
        library.addContent(
                new Attachment().setContentType("application/elm+json").setData(elmJson.getBytes()));

        mockLibrarySearch(bundleWith(library));

        var measureDef = new R4MeasureDefBuilder().build(createMeasureWithPopulation("Initial Population"));
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var result = new R4ExpressionReferenceValidator().validate(context);

        assertTrue(result.isEmpty());
    }

    // --- CompositeMeasureDefValidator tests ---

    @Test
    void compositeValidator_mergesResults() {
        mockLibrarySearch(emptyBundle());

        var measureDef = MeasureDef.fromIdAndUrl(measure.getIdElement(), measure.getUrl());
        var context = new MeasureDefValidationContext(measureDef, measure, repository);

        var composite = new CompositeMeasureDefValidator(
                List.of(new R4CqlLibraryValidator(), new R4ValueSetAvailabilityValidator()));

        var result = composite.validate(context);

        assertTrue(result.hasErrors());
    }

    // --- ValidationResult tests ---

    @Test
    void validationResult_merge() {
        var r1 = new ValidationResult();
        var r2 = new ValidationResult();
        r1.addIssue(new ValidationIssue(ValidationSeverity.ERROR, "CODE1", "desc1", "fix1"));
        r2.addIssue(new ValidationIssue(ValidationSeverity.WARNING, "CODE2", "desc2", "fix2"));

        r1.merge(r2);

        assertEquals(2, r1.getIssues().size());
        assertTrue(r1.hasErrors());
        assertTrue(r1.hasWarnings());
        assertEquals(1, r1.getBlockingErrors().size());
    }

    @Test
    void validationResult_emptyHasNoIssues() {
        var result = new ValidationResult();

        assertTrue(result.isEmpty());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
    }

    // Helper

    private Measure createMeasureWithPopulation(String expressionName) {
        var m = new Measure();
        m.setId("TestMeasure");
        m.setUrl("http://example.com/Measure/TestMeasure");
        m.setLibrary(List.of(new CanonicalType(LIBRARY_URL)));
        m.getScoring()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                .setCode("cohort");

        var group = m.addGroup();
        group.setId("group-1");
        var pop = group.addPopulation();
        pop.setId("initial-population");
        pop.getCode()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
                .setCode("initial-population");
        pop.getCriteria().setLanguage("text/cql-identifier").setExpression(expressionName);

        return m;
    }
}
