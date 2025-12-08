package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;

/**
 * Version-agnostic request object for single-measure evaluation.
 * <p>
 * This class encapsulates all parameters needed for measure evaluation in a
 * version-independent way. Adapters convert this to version-specific types
 * (e.g., R4 types, DSTU3 types).
 * </p>
 * <p>
 * Uses builder pattern for flexible construction with optional parameters.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * MeasureEvaluationRequest request = MeasureEvaluationRequest.builder()
 *     .measureId("MinimalProportionMeasure")
 *     .periodStart(ZonedDateTime.parse("2024-01-01T00:00:00Z"))
 *     .periodEnd(ZonedDateTime.parse("2024-12-31T23:59:59Z"))
 *     .reportType("summary")
 *     .subject("Patient/123")
 *     .build();
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class MeasureEvaluationRequest {

    private final String measureId;

    @Nullable
    private final String measureUrl;

    @Nullable
    private final String measureIdentifier;

    @Nullable
    private final ZonedDateTime periodStart;

    @Nullable
    private final ZonedDateTime periodEnd;

    @Nullable
    private final String reportType;

    @Nullable
    private final String subject;

    @Nullable
    private final String practitioner;

    @Nullable
    private final String productLine;

    @Nullable
    private final String reporter;

    @Nullable
    private final IBaseBundle additionalData;

    @Nullable
    private final IBaseParameters parameters;

    private MeasureEvaluationRequest(Builder builder) {
        this.measureId = builder.measureId;
        this.measureUrl = builder.measureUrl;
        this.measureIdentifier = builder.measureIdentifier;
        this.periodStart = builder.periodStart;
        this.periodEnd = builder.periodEnd;
        this.reportType = builder.reportType;
        this.subject = builder.subject;
        this.practitioner = builder.practitioner;
        this.productLine = builder.productLine;
        this.reporter = builder.reporter;
        this.additionalData = builder.additionalData;
        this.parameters = builder.parameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMeasureId() {
        return measureId;
    }

    @Nullable
    public String getMeasureUrl() {
        return measureUrl;
    }

    @Nullable
    public String getMeasureIdentifier() {
        return measureIdentifier;
    }

    @Nullable
    public ZonedDateTime getPeriodStart() {
        return periodStart;
    }

    @Nullable
    public ZonedDateTime getPeriodEnd() {
        return periodEnd;
    }

    @Nullable
    public String getReportType() {
        return reportType;
    }

    @Nullable
    public String getSubject() {
        return subject;
    }

    @Nullable
    public String getPractitioner() {
        return practitioner;
    }

    @Nullable
    public String getProductLine() {
        return productLine;
    }

    @Nullable
    public String getReporter() {
        return reporter;
    }

    @Nullable
    public IBaseBundle getAdditionalData() {
        return additionalData;
    }

    @Nullable
    public IBaseParameters getParameters() {
        return parameters;
    }

    /**
     * Builder for MeasureEvaluationRequest with fluent API.
     */
    public static class Builder {
        private String measureId;
        private String measureUrl;
        private String measureIdentifier;
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        private String reportType;
        private String subject;
        private String practitioner;
        private String productLine;
        private String reporter;
        private IBaseBundle additionalData;
        private IBaseParameters parameters;

        /**
         * Set the measure ID (e.g., "MinimalProportionMeasure").
         * <p>
         * Exactly one of measureId, measureUrl, or measureIdentifier must be provided.
         * </p>
         */
        public Builder measureId(String measureId) {
            this.measureId = measureId;
            return this;
        }

        /**
         * Set the measure canonical URL (e.g., "http://example.com/Measure/MinimalProportionMeasure").
         */
        public Builder measureUrl(String measureUrl) {
            this.measureUrl = measureUrl;
            return this;
        }

        /**
         * Set the measure identifier (business identifier).
         */
        public Builder measureIdentifier(String measureIdentifier) {
            this.measureIdentifier = measureIdentifier;
            return this;
        }

        /**
         * Set the measurement period start date/time.
         */
        public Builder periodStart(ZonedDateTime periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        /**
         * Set the measurement period start date (convenience method for String parsing).
         */
        public Builder periodStart(String periodStart) {
            this.periodStart = periodStart != null ? ZonedDateTime.parse(periodStart) : null;
            return this;
        }

        /**
         * Set the measurement period end date/time.
         */
        public Builder periodEnd(ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        /**
         * Set the measurement period end date (convenience method for String parsing).
         */
        public Builder periodEnd(String periodEnd) {
            this.periodEnd = periodEnd != null ? ZonedDateTime.parse(periodEnd) : null;
            return this;
        }

        /**
         * Set the report type (e.g., "summary", "subject", "subject-list", "population").
         * If not set, remains null and the measure service will use its default behavior.
         */
        public Builder reportType(String reportType) {
            this.reportType = reportType;
            return this;
        }

        /**
         * Set the subject ID (e.g., "Patient/123" or "Group/cohort-1").
         */
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Set the practitioner ID (e.g., "Practitioner/456").
         */
        public Builder practitioner(String practitioner) {
            this.practitioner = practitioner;
            return this;
        }

        /**
         * Set the product line extension value.
         */
        public Builder productLine(String productLine) {
            this.productLine = productLine;
            return this;
        }

        /**
         * Set the reporter reference.
         */
        public Builder reporter(String reporter) {
            this.reporter = reporter;
            return this;
        }

        /**
         * Set the additional data bundle (version-specific Bundle type).
         */
        public Builder additionalData(IBaseBundle additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        /**
         * Set the parameters resource (version-specific Parameters type).
         */
        public Builder parameters(IBaseParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Build the immutable MeasureEvaluationRequest.
         */
        public MeasureEvaluationRequest build() {
            return new MeasureEvaluationRequest(this);
        }
    }
}
