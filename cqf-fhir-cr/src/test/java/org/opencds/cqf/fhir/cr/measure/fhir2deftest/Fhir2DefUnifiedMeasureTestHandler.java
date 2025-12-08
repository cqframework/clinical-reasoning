package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * Unified test DSL handler for measure evaluation across all FHIR versions (DSTU3, R4, R5, R6+).
 * <p>
 * This DSL handler provides a single, version-agnostic API for testing measure evaluation that works
 * seamlessly across FHIR versions. It supports:
 * </p>
 * <ul>
 *   <li>Single-measure evaluation</li>
 *   <li>Multi-measure evaluation (R4+)</li>
 *   <li>Explicit service selection (force single/multi)</li>
 *   <li>Automatic FHIR version detection</li>
 *   <li>Def object capture and assertion</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Single-measure evaluation (auto-detects FHIR version)
 * Fhir2DefUnifiedMeasureTestHandler.given()
 *     .repositoryFor("MinimalMeasureEvaluation")
 * .when()
 *     .measureId("MinimalProportionMeasure")
 *     .periodStart("2024-01-01")
 *     .periodEnd("2024-12-31")
 *     .reportType("summary")
 *     .captureDef()
 *     .evaluate()
 * .then()
 *     .def()
 *         .hasNoErrors()
 *         .firstGroup()
 *             .population("numerator").hasSubjectCount(7);
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
@SuppressWarnings({"squid:S2699", "squid:S5960"})
public class Fhir2DefUnifiedMeasureTestHandler {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure";

    /**
     * Entry point for test DSL.
     *
     * @return Given builder for setting up test context
     */
    public static Given given() {
        return new Given();
    }

    /**
     * Given phase: Set up test context (repository, FHIR version, options).
     */
    public static class Given {
        private FhirVersionTestContext context;
        private IRepository repository;
        private MeasureEvaluationOptions evaluationOptions;
        private String serverBase;

        public Given() {
            this.evaluationOptions = MeasureEvaluationOptions.defaultOptions();
            // Configure in-memory filtering to avoid repository invoke operations
            this.evaluationOptions
                    .getEvaluationSettings()
                    .getRetrieveSettings()
                    .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                    .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

            this.evaluationOptions
                    .getEvaluationSettings()
                    .getTerminologySettings()
                    .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

            this.serverBase = "http://localhost:8080/fhir";
        }

        /**
         * Set up repository from a test resource path with automatic FHIR version detection.
         *
         * @param repositoryPath relative path to test resources
         * @return this Given builder
         */
        public Given repositoryFor(String repositoryPath) {
            Path fullPath = Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath);
            this.context = FhirVersionTestContext.forRepository(fullPath, this.evaluationOptions, this.serverBase);
            this.repository = this.context.getRepository();
            return this;
        }

        /**
         * Set up repository from a test resource path with explicit FHIR version.
         *
         * @param repositoryPath relative path to test resources
         * @param version explicit FHIR version to use
         * @return this Given builder
         */
        public Given repositoryFor(String repositoryPath, FhirVersionEnum version) {
            Path fullPath = Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath);

            // Create IgRepository with version-specific FhirContext
            this.repository = new IgRepository(FhirContext.forCached(version), fullPath);

            this.context =
                    new FhirVersionTestContext(version, this.repository, this.evaluationOptions, this.serverBase);
            return this;
        }

        /**
         * Provide an explicit repository instance.
         *
         * @param repository the FHIR repository
         * @return this Given builder
         */
        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        /**
         * Configure measure evaluation options.
         *
         * @param evaluationOptions evaluation options
         * @return this Given builder
         */
        public Given evaluationOptions(MeasureEvaluationOptions evaluationOptions) {
            this.evaluationOptions = evaluationOptions;
            return this;
        }

        /**
         * Configure server base URL (for R4 multi-measure service).
         *
         * @param serverBase server base URL
         * @return this Given builder
         */
        public Given serverBase(String serverBase) {
            this.serverBase = serverBase;
            return this;
        }

        /**
         * Transition to When phase.
         *
         * @return When builder
         */
        public When when() {
            if (this.context == null) {
                throw new IllegalStateException(
                        "No repository context configured. Call repositoryFor() before when().");
            }
            return new When(this.context.createMeasureService());
        }
    }

    /**
     * When phase: Configure and execute measure evaluation.
     */
    public static class When {
        private final MeasureServiceAdapter service;
        private final Map<String, MeasureDef> capturedDefs = new HashMap<>();
        private final List<String> measureIds = new ArrayList<>();
        private final List<String> measureUrls = new ArrayList<>();
        private final List<String> measureIdentifiers = new ArrayList<>();

        private EvaluationMode evaluationMode = EvaluationMode.AUTO;

        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private String subject;
        private String reportType;
        private IBaseBundle additionalData;
        private IBaseParameters parameters;
        private String practitioner;
        private String reporter;
        private String productLine;

        private Supplier<Object> operation;

        When(MeasureServiceAdapter service) {
            this.service = service;
        }

        /**
         * Specify a measure by ID.
         *
         * @param measureId the measure ID
         * @return this When builder
         */
        public When measureId(String measureId) {
            this.measureIds.add(measureId);
            return this;
        }

        /**
         * Specify a measure by URL.
         *
         * @param measureUrl the measure canonical URL
         * @return this When builder
         */
        public When measureUrl(String measureUrl) {
            this.measureUrls.add(measureUrl);
            return this;
        }

        /**
         * Specify a measure by identifier.
         *
         * @param measureIdentifier the measure identifier
         * @return this When builder
         */
        public When measureIdentifier(String measureIdentifier) {
            this.measureIdentifiers.add(measureIdentifier);
            return this;
        }

        /**
         * Set measurement period start date (string format).
         *
         * @param periodStart period start date (ISO format: yyyy-MM-dd)
         * @return this When builder
         */
        public When periodStart(String periodStart) {
            this.periodStart = LocalDate.parse(periodStart, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneId.systemDefault());
            return this;
        }

        /**
         * Set measurement period start date (ZonedDateTime).
         *
         * @param periodStart period start date
         * @return this When builder
         */
        public When periodStart(ZonedDateTime periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        /**
         * Set measurement period end date (string format).
         *
         * @param periodEnd period end date (ISO format: yyyy-MM-dd)
         * @return this When builder
         */
        public When periodEnd(String periodEnd) {
            this.periodEnd =
                    LocalDate.parse(periodEnd, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneId.systemDefault());
            return this;
        }

        /**
         * Set measurement period end date (ZonedDateTime).
         *
         * @param periodEnd period end date
         * @return this When builder
         */
        public When periodEnd(ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        /**
         * Set subject ID for subject-level reports.
         *
         * @param subject subject ID
         * @return this When builder
         */
        public When subject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Set report type (summary, subject, subject-list, population).
         *
         * @param reportType report type
         * @return this When builder
         */
        public When reportType(String reportType) {
            this.reportType = reportType;
            return this;
        }

        /**
         * Provide additional data bundle for measure evaluation.
         *
         * @param additionalData additional data bundle
         * @return this When builder
         */
        public When additionalData(IBaseBundle additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        /**
         * Provide parameters for measure evaluation.
         *
         * @param parameters evaluation parameters
         * @return this When builder
         */
        public When parameters(IBaseParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Set practitioner reference.
         *
         * @param practitioner practitioner reference
         * @return this When builder
         */
        public When practitioner(String practitioner) {
            this.practitioner = practitioner;
            return this;
        }

        /**
         * Set reporter reference (for multi-measure).
         *
         * @param reporter reporter reference
         * @return this When builder
         */
        public When reporter(String reporter) {
            this.reporter = reporter;
            return this;
        }

        /**
         * Set product line.
         *
         * @param productLine product line
         * @return this When builder
         */
        public When productLine(String productLine) {
            this.productLine = productLine;
            return this;
        }

        /**
         * Enable Def object capture. Registers a callback to capture MeasureDef state
         * after evaluation (before scoring).
         *
         * @return this When builder
         */
        public When captureDef() {
            service.getMeasureEvaluationOptions().setDefCaptureCallback(def -> {
                String key = def.url() != null ? def.url() : def.id();
                if (key != null) {
                    capturedDefs.put(key, def);
                }
            });
            return this;
        }

        /**
         * Force evaluation using single-measure service (even if multiple measures specified).
         *
         * @return this When builder
         */
        public When evaluateAsSingle() {
            this.evaluationMode = EvaluationMode.FORCE_SINGLE;
            return this;
        }

        /**
         * Force evaluation using multi-measure service (even if single measure specified).
         *
         * @return this When builder
         */
        public When evaluateAsMulti() {
            this.evaluationMode = EvaluationMode.FORCE_MULTI;
            return this;
        }

        /**
         * Execute measure evaluation. Determines whether to use single or multi-measure service
         * based on evaluation mode and number of measures specified.
         *
         * @return this When builder
         */
        public When evaluate() {
            int totalMeasures = measureIds.size() + measureUrls.size() + measureIdentifiers.size();

            if (totalMeasures == 0) {
                throw new IllegalStateException(
                        "No measures specified. Call measureId(), measureUrl(), or measureIdentifier() before evaluate().");
            }

            boolean useSingle = determineUseSingle(totalMeasures);

            if (useSingle) {
                this.operation = this::evaluateSingle;
            } else {
                this.operation = this::evaluateMultiple;
            }

            return this;
        }

        /**
         * Determine whether to use single-measure service based on mode and measure count.
         */
        private boolean determineUseSingle(int totalMeasures) {
            switch (evaluationMode) {
                case FORCE_SINGLE:
                    if (totalMeasures > 1) {
                        throw new IllegalStateException("Cannot force single-measure service with " + totalMeasures
                                + " measures. Remove evaluateAsSingle() or reduce to 1 measure.");
                    }
                    return true;

                case FORCE_MULTI:
                    if (!service.supportsMultiMeasure()) {
                        throw new UnsupportedOperationException(
                                "Multi-measure evaluation not supported for FHIR version: "
                                        + service.getFhirVersion()
                                        + ". Remove evaluateAsMulti() or use R4+.");
                    }
                    return false;

                case AUTO:
                default:
                    // Auto: use single if exactly 1 measure, otherwise multi
                    if (totalMeasures == 1) {
                        return true;
                    } else {
                        if (!service.supportsMultiMeasure()) {
                            throw new UnsupportedOperationException(
                                    "Multi-measure evaluation not supported for FHIR version: "
                                            + service.getFhirVersion()
                                            + ". Specify exactly 1 measure or use R4+.");
                        }
                        return false;
                    }
            }
        }

        /**
         * Execute single-measure evaluation.
         */
        private MeasureReportAdapter evaluateSingle() {
            MeasureEvaluationRequest.Builder requestBuilder = MeasureEvaluationRequest.builder();

            // Set measure identification (prefer ID, then URL)
            if (!measureIds.isEmpty()) {
                requestBuilder.measureId(measureIds.get(0));
            } else if (!measureUrls.isEmpty()) {
                requestBuilder.measureUrl(measureUrls.get(0));
            } else if (!measureIdentifiers.isEmpty()) {
                // Note: Single-measure service may not support identifier lookup
                throw new IllegalArgumentException(
                        "Single-measure service does not support measureIdentifier. Use measureId or measureUrl.");
            }

            // Set period
            if (periodStart != null) {
                requestBuilder.periodStart(periodStart);
            }
            if (periodEnd != null) {
                requestBuilder.periodEnd(periodEnd);
            }

            // Set other parameters
            if (subject != null) {
                requestBuilder.subject(subject);
            }
            if (reportType != null) {
                requestBuilder.reportType(reportType);
            }
            if (additionalData != null) {
                requestBuilder.additionalData(additionalData);
            }
            if (parameters != null) {
                requestBuilder.parameters(parameters);
            }
            if (practitioner != null) {
                requestBuilder.practitioner(practitioner);
            }
            if (productLine != null) {
                requestBuilder.productLine(productLine);
            }

            return service.evaluateSingle(requestBuilder.build());
        }

        /**
         * Execute multi-measure evaluation.
         */
        private MultiMeasureReportAdapter evaluateMultiple() {
            MultiMeasureEvaluationRequest.Builder requestBuilder = MultiMeasureEvaluationRequest.builder();

            // Set measure identifications
            requestBuilder.measureIds(measureIds);
            requestBuilder.measureUrls(measureUrls);
            requestBuilder.measureIdentifiers(measureIdentifiers);

            // Set period
            if (periodStart != null) {
                requestBuilder.periodStart(periodStart);
            }
            if (periodEnd != null) {
                requestBuilder.periodEnd(periodEnd);
            }

            // Set other parameters
            if (subject != null) {
                requestBuilder.subject(subject);
            }
            if (reportType != null) {
                requestBuilder.reportType(reportType);
            }
            if (additionalData != null) {
                requestBuilder.additionalData(additionalData);
            }
            if (parameters != null) {
                requestBuilder.parameters(parameters);
            }
            if (reporter != null) {
                requestBuilder.reporter(reporter);
            }
            if (productLine != null) {
                requestBuilder.productLine(productLine);
            }

            return service.evaluateMultiple(requestBuilder.build());
        }

        /**
         * Transition to Then phase.
         *
         * @return Then builder
         */
        public Then then() {
            if (this.operation == null) {
                throw new IllegalStateException("No operation was selected. Call evaluate() before then().");
            }

            Object result = this.operation.get();
            return new Then(result, capturedDefs, service.getFhirVersion(), this);
        }
    }

    /**
     * Then phase: Assert on evaluation results and captured Def state.
     */
    public static class Then {
        private final Object result;
        private final Map<String, MeasureDef> capturedDefs;
        private final FhirVersionEnum fhirVersion;
        private final When whenContext;

        Then(Object result, Map<String, MeasureDef> capturedDefs, FhirVersionEnum fhirVersion, When whenContext) {
            this.result = result;
            this.capturedDefs = capturedDefs;
            this.fhirVersion = fhirVersion;
            this.whenContext = whenContext;
        }

        /**
         * Get the result (MeasureReportAdapter or MultiMeasureReportAdapter).
         *
         * @return the evaluation result
         */
        public Object result() {
            return result;
        }

        /**
         * Check if result is single-measure report.
         *
         * @return true if single-measure result
         */
        public boolean isSingleMeasure() {
            return result instanceof MeasureReportAdapter;
        }

        /**
         * Check if result is multi-measure report.
         *
         * @return true if multi-measure result
         */
        public boolean isMultiMeasure() {
            return result instanceof MultiMeasureReportAdapter;
        }

        /**
         * Get single-measure report.
         *
         * @return the MeasureReportAdapter
         * @throws IllegalStateException if result is not single-measure
         */
        public MeasureReportAdapter singleReport() {
            if (!isSingleMeasure()) {
                throw new IllegalStateException("Result is not a single-measure report");
            }
            return (MeasureReportAdapter) result;
        }

        /**
         * Get multi-measure report.
         *
         * @return the MultiMeasureReportAdapter
         * @throws IllegalStateException if result is not multi-measure
         */
        public MultiMeasureReportAdapter multiReport() {
            if (!isMultiMeasure()) {
                throw new IllegalStateException("Result is not a multi-measure report");
            }
            return (MultiMeasureReportAdapter) result;
        }

        /**
         * Assert on captured Def state (for single-measure or multi with N=1).
         *
         * @return SelectedDef for assertions
         */
        public SelectedDef def() {
            if (capturedDefs.isEmpty()) {
                throw new IllegalStateException("No Def captured. Did you call captureDef() before evaluate()?");
            }

            if (capturedDefs.size() > 1) {
                throw new IllegalStateException("Multiple Defs captured (" + capturedDefs.size()
                        + "). Use def(measureUrl) to specify which Def to assert on.");
            }

            return new SelectedDef(capturedDefs.values().iterator().next(), this);
        }

        /**
         * Assert on captured Def state by measure URL (for multi-measure).
         *
         * @param measureUrl the measure URL
         * @return SelectedDef for assertions
         */
        public SelectedDef def(String measureUrl) {
            MeasureDef def = capturedDefs.get(measureUrl);
            if (def == null) {
                throw new IllegalArgumentException("No Def captured for measure URL: " + measureUrl
                        + ". Available URLs: " + capturedDefs.keySet());
            }
            return new SelectedDef(def, this);
        }

        /**
         * Get FHIR version of evaluation.
         *
         * @return FHIR version enum
         */
        public FhirVersionEnum fhirVersion() {
            return fhirVersion;
        }
    }

    /**
     * Evaluation mode enum.
     */
    public enum EvaluationMode {
        /**
         * Automatically determine single vs multi based on measure count.
         */
        AUTO,

        /**
         * Force single-measure service (error if multiple measures specified).
         */
        FORCE_SINGLE,

        /**
         * Force multi-measure service (even if single measure specified).
         */
        FORCE_MULTI
    }
}
