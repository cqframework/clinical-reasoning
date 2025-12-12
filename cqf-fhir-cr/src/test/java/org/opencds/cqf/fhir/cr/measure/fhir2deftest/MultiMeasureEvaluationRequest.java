package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;

/**
 * Version-agnostic request object for multi-measure evaluation.
 * <p>
 * This class encapsulates all parameters needed for evaluating multiple measures
 * simultaneously. Adapters convert this to version-specific types.
 * </p>
 * <p>
 * <strong>Note:</strong> Multi-measure evaluation is NOT supported in DSTU3.
 * Attempting to use this with a DSTU3 adapter will throw {@link UnsupportedOperationException}.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * MultiMeasureEvaluationRequest request = MultiMeasureEvaluationRequest.builder()
 *     .measureIds(List.of("Measure1", "Measure2", "Measure3"))
 *     .periodStart(ZonedDateTime.parse("2024-01-01T00:00:00Z"))
 *     .periodEnd(ZonedDateTime.parse("2024-12-31T23:59:59Z"))
 *     .reportType("population")
 *     .build();
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class MultiMeasureEvaluationRequest {

    private final List<String> measureIds;
    private final List<String> measureUrls;
    private final List<String> measureIdentifiers;

    @Nullable
    private final ZonedDateTime periodStart;

    @Nullable
    private final ZonedDateTime periodEnd;

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

    private MultiMeasureEvaluationRequest(Builder builder) {
        this.measureIds = new ArrayList<>(builder.measureIds);
        this.measureUrls = new ArrayList<>(builder.measureUrls);
        this.measureIdentifiers = new ArrayList<>(builder.measureIdentifiers);
        this.periodStart = builder.periodStart;
        this.periodEnd = builder.periodEnd;
        this.reportType = builder.reportType != null ? builder.reportType : "summary";
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

    public List<String> getMeasureIds() {
        return measureIds;
    }

    public List<String> getMeasureUrls() {
        return measureUrls;
    }

    public List<String> getMeasureIdentifiers() {
        return measureIdentifiers;
    }

    @Nullable
    public ZonedDateTime getPeriodStart() {
        return periodStart;
    }

    @Nullable
    public ZonedDateTime getPeriodEnd() {
        return periodEnd;
    }

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
     * Builder for MultiMeasureEvaluationRequest with fluent API.
     */
    public static class Builder {
        private List<String> measureIds = new ArrayList<>();
        private List<String> measureUrls = new ArrayList<>();
        private List<String> measureIdentifiers = new ArrayList<>();
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
         * Add a single measure ID to evaluate.
         */
        public Builder measureId(String measureId) {
            this.measureIds.add(measureId);
            return this;
        }

        /**
         * Set all measure IDs to evaluate (replaces any previously added).
         */
        public Builder measureIds(List<String> measureIds) {
            this.measureIds = new ArrayList<>(measureIds);
            return this;
        }

        /**
         * Add a single measure canonical URL to evaluate.
         */
        public Builder measureUrl(String measureUrl) {
            this.measureUrls.add(measureUrl);
            return this;
        }

        /**
         * Set all measure canonical URLs to evaluate (replaces any previously added).
         */
        public Builder measureUrls(List<String> measureUrls) {
            this.measureUrls = new ArrayList<>(measureUrls);
            return this;
        }

        /**
         * Add a single measure identifier to evaluate.
         */
        public Builder measureIdentifier(String measureIdentifier) {
            this.measureIdentifiers.add(measureIdentifier);
            return this;
        }

        /**
         * Set all measure identifiers to evaluate (replaces any previously added).
         */
        public Builder measureIdentifiers(List<String> measureIdentifiers) {
            this.measureIdentifiers = new ArrayList<>(measureIdentifiers);
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
         * Defaults to "summary" if not set.
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
         * Build the immutable MultiMeasureEvaluationRequest.
         */
        public MultiMeasureEvaluationRequest build() {
            return new MultiMeasureEvaluationRequest(this);
        }
    }
}
