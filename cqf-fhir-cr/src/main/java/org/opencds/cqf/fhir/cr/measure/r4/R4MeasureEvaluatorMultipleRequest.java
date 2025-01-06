package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;

/**
 * Object to capture parameters for evaluating multiple measures
 */
public class R4MeasureEvaluatorMultipleRequest {

    private final List<IdType> measureId;
    private final List<String> measureUrl;
    private final List<String> measureIdentifier;

    @Nullable
    private final ZonedDateTime periodStart;

    @Nullable
    private final ZonedDateTime periodEnd;

    private final String reportType;
    private final String subject;
    private final Endpoint contentEndpoint;
    private final Endpoint terminologyEndpoint;
    private final Endpoint dataEndpoint;
    private final Bundle additionalData;
    private final Parameters parameters;
    private final String productLine;
    private final String reporter;

    public R4MeasureEvaluatorMultipleRequest(Builder builder) {
        this.measureId = builder.measureId;
        this.measureUrl = builder.measureUrl;
        this.measureIdentifier = builder.measureIdentifier;
        this.periodStart = builder.periodStart;
        this.periodEnd = builder.periodEnd;
        this.reportType = builder.reportType;
        this.subject = builder.subject;
        this.contentEndpoint = builder.contentEndpoint;
        this.terminologyEndpoint = builder.terminologyEndpoint;
        this.dataEndpoint = builder.dataEndpoint;
        this.additionalData = builder.additionalData;
        this.parameters = builder.parameters;
        this.productLine = builder.productLine;
        this.reporter = builder.reporter;
    }

    public List<IdType> getMeasureId() {
        return measureId;
    }

    public List<String> getMeasureUrl() {
        return measureUrl;
    }

    public List<String> getMeasureIdentifier() {
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

    public String getReportType() {
        return reportType;
    }

    public String getSubject() {
        return subject;
    }

    public Endpoint getContentEndpoint() {
        return contentEndpoint;
    }

    public Endpoint getTerminologyEndpoint() {
        return terminologyEndpoint;
    }

    public Endpoint getDataEndpoint() {
        return dataEndpoint;
    }

    public Bundle getAdditionalData() {
        return additionalData;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public String getProductLine() {
        return productLine;
    }

    public String getReporter() {
        return reporter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<IdType> measureId;
        private List<String> measureUrl;
        private List<String> measureIdentifier;

        @Nullable
        private ZonedDateTime periodStart;

        @Nullable
        private ZonedDateTime periodEnd;

        private String reportType;
        private String subject;
        private Endpoint contentEndpoint;
        private Endpoint terminologyEndpoint;
        private Endpoint dataEndpoint;
        private Bundle additionalData;
        private Parameters parameters;
        private String productLine;
        private String reporter;

        public Builder setMeasureId(List<IdType> measureId) {
            this.measureId = measureId;
            return this;
        }

        public Builder setMeasureUrl(List<String> measureUrl) {
            this.measureUrl = measureUrl;
            return this;
        }

        public Builder setMeasureIdentifier(List<String> measureIdentifier) {
            this.measureIdentifier = measureIdentifier;
            return this;
        }

        public Builder setPeriodStart(@Nullable ZonedDateTime periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        public Builder setPeriodEnd(@Nullable ZonedDateTime periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        public Builder setReportType(String reportType) {
            this.reportType = reportType;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder setContentEndpoint(Endpoint contentEndpoint) {
            this.contentEndpoint = contentEndpoint;
            return this;
        }

        public Builder setTerminologyEndpoint(Endpoint terminologyEndpoint) {
            this.terminologyEndpoint = terminologyEndpoint;
            return this;
        }

        public Builder setDataEndpoint(Endpoint dataEndpoint) {
            this.dataEndpoint = dataEndpoint;
            return this;
        }

        public Builder setAdditionalData(Bundle additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public Builder setParameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder setProductLine(String productLine) {
            this.productLine = productLine;
            return this;
        }

        public Builder setReporter(String reporter) {
            this.reporter = reporter;
            return this;
        }

        public R4MeasureEvaluatorMultipleRequest build() {
            return new R4MeasureEvaluatorMultipleRequest(this);
        }
    }
}
