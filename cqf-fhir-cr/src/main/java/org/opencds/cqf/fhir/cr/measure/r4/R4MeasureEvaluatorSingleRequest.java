package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.utility.monad.Either3;
import java.time.ZonedDateTime;

// LUKETODO: javadoc
public class R4MeasureEvaluatorSingleRequest {
    private final Either3<CanonicalType, IdType, Measure> measure;
    @Nullable
    private final ZonedDateTime periodStart;
    @Nullable
    private final ZonedDateTime periodEnd;
    private final String reportType;
    private final String subjectId;
    private final String lastReceivedOn;
    private final Endpoint contentEndpoint;
    private final Endpoint terminologyEndpoint;
    private final Endpoint dataEndpoint;
    private final Bundle additionalData;
    private final Parameters parameters;
    private final String productLine;
    private final String practitioner;

    private R4MeasureEvaluatorSingleRequest(Builder builder) {
        measure = builder.measure;
        periodStart = builder.periodStart;
        periodEnd = builder.periodEnd;
        reportType = builder.reportType;
        subjectId = builder.subjectId;
        lastReceivedOn = builder.lastReceivedOn;
        contentEndpoint = builder.contentEndpoint;
        terminologyEndpoint = builder.terminologyEndpoint;
        dataEndpoint = builder.dataEndpoint;
        additionalData = builder.additionalData;
        parameters = builder.parameters;
        productLine = builder.productLine;
        practitioner = builder.practitioner;
    }

    public Either3<CanonicalType, IdType, Measure> getMeasure() {
        return measure;
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

    public String getSubjectId() {
        return subjectId;
    }

    public String getLastReceivedOn() {
        return lastReceivedOn;
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

    public String getPractitioner() {
        return practitioner;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Either3<CanonicalType, IdType, Measure> measure;
        @Nullable
        private ZonedDateTime periodStart;
        @Nullable
        private ZonedDateTime periodEnd;
        private String reportType;
        private String subjectId;
        private String lastReceivedOn;
        private Endpoint contentEndpoint;
        private Endpoint terminologyEndpoint;
        private Endpoint dataEndpoint;
        private Bundle additionalData;
        private Parameters parameters;
        private String productLine;
        private String practitioner;

        public Builder setMeasure(Either3<CanonicalType, IdType, Measure> measure) {
            this.measure = measure;
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

        public Builder setSubjectId(String subjectId) {
            this.subjectId = subjectId;
            return this;
        }

        public Builder setLastReceivedOn(String lastReceivedOn) {
            this.lastReceivedOn = lastReceivedOn;
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

        public Builder setPractitioner(String practitioner) {
            this.practitioner = practitioner;
            return this;
        }

        public R4MeasureEvaluatorSingleRequest build() {
            return new R4MeasureEvaluatorSingleRequest(this);
        }
    }
}
