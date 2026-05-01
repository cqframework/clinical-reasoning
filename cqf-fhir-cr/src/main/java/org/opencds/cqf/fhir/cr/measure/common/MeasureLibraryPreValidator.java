package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.ValueSetDef;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static pre-validation pass for measure libraries. Resolves each library, captures fatal compile
 * errors, and walks the compiled ELM ValueSet references to confirm they exist in the terminology
 * provider — all before any per-subject CQL evaluation runs.
 *
 * <p>Libraries with fatal compile errors or unresolvable ValueSet references are recorded as
 * errors against their bound measure defs and returned in the failed-set so the caller can
 * exclude them from per-subject CQL evaluation. This avoids two failure modes that would
 * otherwise occur at runtime:
 * <ul>
 *   <li><b>Diagnostic masking</b>: function-based stratifier evaluation observes missing
 *       expression results when the IP fails and throws a higher-level
 *       "Expression result: &lt;pop&gt; is missing" error that hides the underlying cause.</li>
 *   <li><b>Multi-measure pollution</b>: the masked exception escapes the inner library loop in
 *       {@link MeasureEvaluationResultHandler} and is caught by the outer subject-scoped
 *       try/catch, which writes the error to <em>every</em> measure def — including unrelated
 *       libraries that compiled and would have evaluated cleanly.</li>
 * </ul>
 *
 * <p>Hard library-resolution failures (e.g. {@code CqlIncludeException} for a missing CQL source)
 * still propagate to the runtime CQL path so existing throw-based contracts in
 * {@code InvalidMeasureTest} are preserved.
 */
public class MeasureLibraryPreValidator {

    private static final Logger logger = LoggerFactory.getLogger(MeasureLibraryPreValidator.class);
    private static final String EXCEPTION_FOR_LIBRARY_TEMPLATE = "Exception for library: %s, Message: %s";

    private final CqlEngine engine;

    public MeasureLibraryPreValidator(CqlEngine engine) {
        this.engine = engine;
    }

    /**
     * Validate compile-time integrity for every library bound to the given measure defs.
     *
     * @param details measure-engine details holding the library-to-measure binding
     * @param resultsBuilder builder to record per-library validation errors against measure defs
     * @return library identifiers that failed validation (caller should exclude them from
     *         per-subject CQL evaluation)
     */
    public Set<VersionedIdentifier> validate(
            MultiLibraryIdMeasureEngineDetails details, CompositeEvaluationResultsPerMeasure.Builder resultsBuilder) {

        var libraryManager = engine.getEnvironment().getLibraryManager();
        var terminologyProvider = engine.getEnvironment().getTerminologyProvider();
        var failed = new HashSet<VersionedIdentifier>();

        for (var libraryId : details.getLibraryIdentifiers()) {
            findFirstFailure(libraryManager, terminologyProvider, libraryId)
                    .ifPresent(error -> recordLibraryFailure(details, resultsBuilder, libraryId, error, failed));
        }
        return failed;
    }

    private static Optional<RuntimeException> findFirstFailure(
            LibraryManager libraryManager, TerminologyProvider terminologyProvider, VersionedIdentifier libraryId) {

        var compileErrors = new ArrayList<CqlCompilerException>();
        CompiledLibrary compiled;
        try {
            compiled = libraryManager.resolveLibrary(libraryId, compileErrors);
        } catch (RuntimeException exception) {
            // Hard failures (missing library source, etc.) preserve the existing throw-at-runtime
            // contract for callers that rely on the exception type.
            return Optional.empty();
        }

        var firstCompileError = firstFatalCompileError(compileErrors);
        if (firstCompileError.isPresent()) {
            return firstCompileError;
        }

        return findFirstUnresolvableValueSet(terminologyProvider, compiled);
    }

    private static Optional<RuntimeException> firstFatalCompileError(List<CqlCompilerException> errors) {
        return errors.stream()
                .filter(exception -> exception.getSeverity() == CqlCompilerException.ErrorSeverity.Error)
                .map(RuntimeException.class::cast)
                .findFirst();
    }

    private static Optional<RuntimeException> findFirstUnresolvableValueSet(
            TerminologyProvider terminologyProvider, CompiledLibrary compiled) {

        for (var vsDef : valueSetDefs(compiled)) {
            var info = toValueSetInfo(vsDef);
            if (info != null) {
                try {
                    terminologyProvider.expand(info);
                } catch (RuntimeException e) {
                    return Optional.of(e);
                }
            }
        }
        return Optional.empty();
    }

    private static List<ValueSetDef> valueSetDefs(CompiledLibrary compiled) {
        if (compiled == null || compiled.getLibrary() == null) {
            return List.of();
        }
        var valueSets = compiled.getLibrary().getValueSets();
        return valueSets == null ? List.of() : valueSets.getDef();
    }

    private static ValueSetInfo toValueSetInfo(ValueSetDef vsDef) {
        if (vsDef.getId() == null) {
            return null;
        }
        var info = new ValueSetInfo().withId(vsDef.getId());
        if (vsDef.getVersion() != null) {
            info.setVersion(vsDef.getVersion());
        }
        return info;
    }

    private static void recordLibraryFailure(
            MultiLibraryIdMeasureEngineDetails details,
            CompositeEvaluationResultsPerMeasure.Builder resultsBuilder,
            VersionedIdentifier libraryId,
            RuntimeException error,
            Set<VersionedIdentifier> failed) {
        var measureDefs = details.getMeasureDefsForLibrary(libraryId);
        var formatted = EXCEPTION_FOR_LIBRARY_TEMPLATE.formatted(libraryId.getId(), error.getMessage());
        resultsBuilder.addErrors(measureDefs, formatted);
        logger.error(formatted, error);
        failed.add(libraryId);
    }
}
