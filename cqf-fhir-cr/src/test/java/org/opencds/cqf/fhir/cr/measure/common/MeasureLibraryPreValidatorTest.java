package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlSemanticException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.Library;
import org.hl7.elm.r1.ValueSetDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Environment;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MeasureLibraryPreValidatorTest {

    private static final VersionedIdentifier LIB_A = new VersionedIdentifier().withId("LibA");
    private static final VersionedIdentifier LIB_B = new VersionedIdentifier().withId("LibB");
    private static final VersionedIdentifier LIB_C = new VersionedIdentifier().withId("LibC");

    private static final MeasureDef MEASURE_DEF_A = measureDef("MeasureA");
    private static final MeasureDef MEASURE_DEF_B = measureDef("MeasureB");
    private static final MeasureDef MEASURE_DEF_C = measureDef("MeasureC");

    @Mock
    private CqlEngine engine;

    @Mock
    private Environment environment;

    @Mock
    private LibraryManager libraryManager;

    @Mock
    private TerminologyProvider terminologyProvider;

    private MeasureLibraryPreValidator subject;

    @BeforeEach
    void setUp() {
        when(engine.getEnvironment()).thenReturn(environment);
        when(environment.getLibraryManager()).thenReturn(libraryManager);
        when(environment.getTerminologyProvider()).thenReturn(terminologyProvider);
        subject = new MeasureLibraryPreValidator(engine);
    }

    @Test
    void noLibraries_returnsEmptyFailedSet() {
        var details = MultiLibraryIdMeasureEngineDetails.builder(null).build();
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertTrue(failed.isEmpty());
        assertTrue(resultsBuilder.build().getErrorsPerMeasure().isEmpty());
    }

    @Test
    void cleanLibrary_withNoValueSets_passes() {
        var details = detailsFor(LIB_A, MEASURE_DEF_A);
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList())).thenReturn(compiledWithoutValueSets());
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertTrue(failed.isEmpty());
        assertTrue(resultsBuilder.build().getErrorsPerMeasure().isEmpty());
        verify(terminologyProvider, never()).expand(any());
    }

    @Test
    void fatalCompileError_flagsLibraryAndRecordsError() {
        var details = detailsFor(LIB_A, MEASURE_DEF_A);
        var compileError = new CqlSemanticException(
                "symbol foo not defined", null, CqlCompilerException.ErrorSeverity.Error, null);
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList())).thenAnswer(inv -> {
            List<CqlCompilerException> errors = inv.getArgument(1);
            errors.add(compileError);
            return compiledWithoutValueSets();
        });
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertEquals(Set.of(LIB_A), failed);
        var errorsForA = resultsBuilder.build().getErrorsPerMeasure().get(MEASURE_DEF_A);
        assertEquals(1, errorsForA.size());
        assertTrue(errorsForA.get(0).contains("Exception for library: LibA"));
        assertTrue(errorsForA.get(0).contains("symbol foo not defined"));
    }

    @Test
    void warningOnlyCompileErrors_doNotFlagLibrary() {
        var details = detailsFor(LIB_A, MEASURE_DEF_A);
        var warning =
                new CqlSemanticException("deprecated function", null, CqlCompilerException.ErrorSeverity.Warning, null);
        var info = new CqlSemanticException("info note", null, CqlCompilerException.ErrorSeverity.Info, null);
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList())).thenAnswer(inv -> {
            List<CqlCompilerException> errors = inv.getArgument(1);
            errors.add(warning);
            errors.add(info);
            return compiledWithoutValueSets();
        });
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertTrue(failed.isEmpty());
        assertTrue(resultsBuilder.build().getErrorsPerMeasure().isEmpty());
    }

    @Test
    void unresolvableValueSet_flagsLibraryAndRecordsError() {
        var details = detailsFor(LIB_A, MEASURE_DEF_A);
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList()))
                .thenReturn(compiledWithValueSets("http://example.com/ValueSet/missing"));
        when(terminologyProvider.expand(any(ValueSetInfo.class)))
                .thenThrow(
                        new IllegalArgumentException("Unable to locate ValueSet http://example.com/ValueSet/missing"));
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertEquals(Set.of(LIB_A), failed);
        var errorsForA = resultsBuilder.build().getErrorsPerMeasure().get(MEASURE_DEF_A);
        assertEquals(1, errorsForA.size());
        assertTrue(errorsForA.get(0).contains("Exception for library: LibA"));
        assertTrue(errorsForA.get(0).contains("Unable to locate ValueSet"));
    }

    @Test
    void resolveLibraryThrowing_fallsThroughToRuntime() {
        var details = detailsFor(LIB_A, MEASURE_DEF_A);
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList()))
                .thenThrow(new IllegalStateException("hard library-resolution failure"));
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertTrue(failed.isEmpty());
        assertTrue(resultsBuilder.build().getErrorsPerMeasure().isEmpty());
        verify(terminologyProvider, never()).expand(any());
    }

    @Test
    void firstValueSetFailure_shortCircuitsRemainingChecks() {
        var details = detailsFor(LIB_A, MEASURE_DEF_A);
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList()))
                .thenReturn(compiledWithValueSets(
                        "http://example.com/ValueSet/missing-1", "http://example.com/ValueSet/missing-2"));
        when(terminologyProvider.expand(any(ValueSetInfo.class)))
                .thenThrow(new IllegalArgumentException(
                        "Unable to locate ValueSet http://example.com/ValueSet/missing-1"));
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertEquals(Set.of(LIB_A), failed);
        // Exactly one error recorded — the validator stops at the first unresolvable ValueSet.
        var errorsForA = resultsBuilder.build().getErrorsPerMeasure().get(MEASURE_DEF_A);
        assertEquals(1, errorsForA.size());
        verify(terminologyProvider).expand(any(ValueSetInfo.class));
    }

    @Test
    void valueSetDefWithNullId_isSkippedNotFailed() {
        var details = detailsFor(LIB_A, MEASURE_DEF_A);
        var library = new Library();
        var valueSets = new Library.ValueSets();
        valueSets.getDef().add(new ValueSetDef()); // id deliberately left null
        library.setValueSets(valueSets);
        var compiled = new CompiledLibrary();
        compiled.setLibrary(library);
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList())).thenReturn(compiled);
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertTrue(failed.isEmpty());
        assertTrue(resultsBuilder.build().getErrorsPerMeasure().isEmpty());
        verify(terminologyProvider, never()).expand(any());
    }

    @Test
    void multipleLibraries_isolation_onlyFailingOneFlagged() {
        var details = MultiLibraryIdMeasureEngineDetails.builder(null)
                .addLibraryIdToMeasureId(LIB_A, MEASURE_DEF_A)
                .addLibraryIdToMeasureId(LIB_B, MEASURE_DEF_B)
                .addLibraryIdToMeasureId(LIB_C, MEASURE_DEF_C)
                .build();
        when(libraryManager.resolveLibrary(eq(LIB_A), anyList())).thenReturn(compiledWithoutValueSets());
        when(libraryManager.resolveLibrary(eq(LIB_B), anyList()))
                .thenReturn(compiledWithValueSets("http://example.com/ValueSet/b-missing"));
        when(libraryManager.resolveLibrary(eq(LIB_C), anyList())).thenReturn(compiledWithoutValueSets());
        when(terminologyProvider.expand(any(ValueSetInfo.class)))
                .thenThrow(new IllegalArgumentException(
                        "Unable to locate ValueSet http://example.com/ValueSet/b-missing"));
        var resultsBuilder = CompositeEvaluationResultsPerMeasure.builder();

        var failed = subject.validate(details, resultsBuilder);

        assertEquals(Set.of(LIB_B), failed);
        var errorsPerMeasure = resultsBuilder.build().getErrorsPerMeasure();
        // Only B receives an error; A and C remain untouched.
        assertFalse(errorsPerMeasure.containsKey(MEASURE_DEF_A));
        assertFalse(errorsPerMeasure.containsKey(MEASURE_DEF_C));
        var errorsForB = errorsPerMeasure.get(MEASURE_DEF_B);
        assertEquals(1, errorsForB.size());
        assertTrue(errorsForB.get(0).contains("Unable to locate ValueSet"));
    }

    private static MultiLibraryIdMeasureEngineDetails detailsFor(VersionedIdentifier libraryId, MeasureDef measureDef) {
        return MultiLibraryIdMeasureEngineDetails.builder(null)
                .addLibraryIdToMeasureId(libraryId, measureDef)
                .build();
    }

    private static MeasureDef measureDef(String id) {
        return MeasureDef.fromIdAndUrl(new IdType(ResourceType.Measure.name(), id), "http://example.com/Measure/" + id);
    }

    private static CompiledLibrary compiledWithoutValueSets() {
        var compiled = new CompiledLibrary();
        compiled.setLibrary(new Library());
        return compiled;
    }

    private static CompiledLibrary compiledWithValueSets(String... urls) {
        var library = new Library();
        var valueSets = new Library.ValueSets();
        for (var url : urls) {
            valueSets.getDef().add(new ValueSetDef().withId(url));
        }
        library.setValueSets(valueSets);
        var compiled = new CompiledLibrary();
        compiled.setLibrary(library);
        return compiled;
    }
}
