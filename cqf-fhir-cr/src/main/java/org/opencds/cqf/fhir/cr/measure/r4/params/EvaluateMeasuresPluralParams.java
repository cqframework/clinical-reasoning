/*-
 * #%L
 * Smile CDR - CDR
 * %%
 * Copyright (C) 2016 - 2025 Smile CDR, Inc.
 * %%
 * All rights reserved.
 * #L%
 */
package org.opencds.cqf.fhir.cr.measure.r4.params;

import ca.uhn.fhir.rest.annotation.EmbeddableOperationParams;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OperationParameterRangeType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Non-RequestDetails parameters Non-standard enhancement to $evaluate-measure operation, which allows end users to pass multiple measureIds to the operation and
 * populate 'MeasureReport.reporter' directly with parameter.
 * <p/>
 * myMeasureId             the ids of the Measures to evaluate
 * myMeasureIdentifier the System url & accompanying value, or just value, associated with Measure(s)
 * myMeasureUrl 	the canonical Url of the Measure resource to evaluate Measure(s)
 * myPeriodStart    The start of the reporting period
 * myPeriodEnd      The end of the reporting period
 * myReportType     The type of MeasureReport to generate
 * theSubject        the subject to use for the evaluation
 * theProductLine    the productLine (e.g. Medicare, Medicaid, etc) to use
 *                          for the evaluation. This is a non-standard parameter.
 * theAdditionalData the data bundle containing additional data
 */
@EmbeddableOperationParams
public class EvaluateMeasuresPluralParams {
	private final List<IdType> myMeasureId;

	private final List<String> myMeasureIdentifier;

	private final List<String> myMeasureUrl;

	private final ZonedDateTime myPeriodStart;

	private final ZonedDateTime myPeriodEnd;

	private final String myReportType;

	private final String mySubject;

	private final String myProductLine;

	private final Bundle myAdditionalData;

	private final Endpoint myTerminologyEndpoint;

	private final Parameters myParameters;

	private final String myReporter;

	public EvaluateMeasuresPluralParams(
			@OperationParam(name = "measureId") List<IdType> theMeasureId,
			@OperationParam(name = "measureIdentifier") List<String> theMeasureIdentifier,
			@OperationParam(name = "measureUrl") List<String> theMeasureUrl,
			@OperationParam(
							name = "periodStart",
							sourceType = String.class,
							rangeType = OperationParameterRangeType.START)
					ZonedDateTime thePeriodStart,
			@OperationParam(name = "periodEnd", sourceType = String.class, rangeType = OperationParameterRangeType.END)
					ZonedDateTime thePeriodEnd,
			@OperationParam(name = "reportType") String theReportType,
			@OperationParam(name = "subject") String theSubject,
			@OperationParam(name = "productLine") String theProductLine,
			@OperationParam(name = "additionalData") Bundle theAdditionalData,
			@OperationParam(name = "terminologyEndpoint") Endpoint theTerminologyEndpoint,
			@OperationParam(name = "parameters") Parameters theParameters,
			@OperationParam(name = "reporter") String theReporter) {
		myMeasureId = theMeasureId;
		myMeasureIdentifier = theMeasureIdentifier;
		myMeasureUrl = theMeasureUrl;
		myPeriodStart = thePeriodStart;
		myPeriodEnd = thePeriodEnd;
		myReportType = theReportType;
		mySubject = theSubject;
		myProductLine = theProductLine;
		myAdditionalData = theAdditionalData;
		myTerminologyEndpoint = theTerminologyEndpoint;
		myParameters = theParameters;
		myReporter = theReporter;
	}

	public static Builder builder() {
		return new Builder();
	}

	private EvaluateMeasuresPluralParams(Builder builder) {
        this(
            builder.myMeasureId,
            builder.myMeasureIdentifier,
            builder.myMeasureUrl,
            builder.myPeriodStart,
            builder.myPeriodEnd,
            builder.myReportType,
            builder.mySubject,
            builder.myProductLine,
            builder.myAdditionalData,
            builder.myTerminologyEndpoint,
            builder.myParameters,
            builder.myReporter
        );
	}

	public List<IdType> getMeasureId() {
		return myMeasureId;
	}

	public List<String> getMeasureIdentifier() {
		return myMeasureIdentifier;
	}

	public List<String> getMeasureUrl() {
		return myMeasureUrl;
	}

	public ZonedDateTime getPeriodStart() {
		return myPeriodStart;
	}

	public ZonedDateTime getPeriodEnd() {
		return myPeriodEnd;
	}

	public String getReportType() {
		return myReportType;
	}

	public String getSubject() {
		return mySubject;
	}

	public String getProductLine() {
		return myProductLine;
	}

	public Bundle getAdditionalData() {
		return myAdditionalData;
	}

	public Endpoint getTerminologyEndpoint() {
		return myTerminologyEndpoint;
	}

	public Parameters getParameters() {
		return myParameters;
	}

	public String getReporter() {
		return myReporter;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EvaluateMeasuresPluralParams that = (EvaluateMeasuresPluralParams) o;
		return Objects.equals(myMeasureId, that.myMeasureId)
				&& Objects.equals(myMeasureIdentifier, that.myMeasureIdentifier)
				&& Objects.equals(myMeasureUrl, that.myMeasureUrl)
				&& Objects.equals(myPeriodStart, that.myPeriodStart)
				&& Objects.equals(myPeriodEnd, that.myPeriodEnd)
				&& Objects.equals(myReportType, that.myReportType)
				&& Objects.equals(mySubject, that.mySubject)
				&& Objects.equals(myProductLine, that.myProductLine)
				&& Objects.equals(myAdditionalData, that.myAdditionalData)
				&& Objects.equals(myTerminologyEndpoint, that.myTerminologyEndpoint)
				&& Objects.equals(myParameters, that.myParameters)
				&& Objects.equals(myReporter, that.myReporter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				myMeasureId,
				myMeasureIdentifier,
				myMeasureUrl,
				myPeriodStart,
				myPeriodEnd,
				myReportType,
				mySubject,
				myProductLine,
				myAdditionalData,
				myTerminologyEndpoint,
				myParameters,
				myReporter);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", EvaluateMeasuresPluralParams.class.getSimpleName() + "[", "]")
				.add("myMeasureId=" + myMeasureId)
				.add("myMeasureIdentifier=" + myMeasureIdentifier)
				.add("myMeasureUrl=" + myMeasureUrl)
				.add("myPeriodStart='" + myPeriodStart + "'")
				.add("myPeriodEnd='" + myPeriodEnd + "'")
				.add("myReportType='" + myReportType + "'")
				.add("mySubject='" + mySubject + "'")
				.add("myProductLine='" + myProductLine + "'")
				.add("myAdditionalData=" + myAdditionalData)
				.add("myTerminologyEndpoint=" + myTerminologyEndpoint)
				.add("myParameters=" + myParameters)
				.add("myReporter='" + myReporter + "'")
				.toString();
	}

	public static class Builder {
		private List<IdType> myMeasureId;
		private List<String> myMeasureIdentifier;
		private List<String> myMeasureUrl;
		private ZonedDateTime myPeriodStart;
		private ZonedDateTime myPeriodEnd;
		private String myReportType;
		private String mySubject;
		private String myProductLine;
		private Bundle myAdditionalData;
		private Endpoint myTerminologyEndpoint;
		private Parameters myParameters;
		private String myReporter;

		public Builder setMeasureId(List<IdType> theMeasureId) {
			myMeasureId = theMeasureId;
			return this;
		}

		public Builder setMeasureIdentifier(List<String> theMeasureIdentifier) {
			myMeasureIdentifier = theMeasureIdentifier;
			return this;
		}

		public Builder setMeasureUrl(List<String> theMeasureUrl) {
			myMeasureUrl = theMeasureUrl;
			return this;
		}

		public Builder setPeriodStart(ZonedDateTime thePeriodStart) {
			myPeriodStart = thePeriodStart;
			return this;
		}

		public Builder setPeriodEnd(ZonedDateTime thePeriodEnd) {
			myPeriodEnd = thePeriodEnd;
			return this;
		}

		public Builder setReportType(String theReportType) {
			myReportType = theReportType;
			return this;
		}

		public Builder setSubject(String theSubject) {
			mySubject = theSubject;
			return this;
		}

		public Builder setProductLine(String theProductLine) {
			myProductLine = theProductLine;
			return this;
		}

		public Builder setAdditionalData(Bundle theAdditionalData) {
			myAdditionalData = theAdditionalData;
			return this;
		}

		public Builder setTerminologyEndpoint(Endpoint theTerminologyEndpoint) {
			myTerminologyEndpoint = theTerminologyEndpoint;
			return this;
		}

		public Builder setParameters(Parameters theParameters) {
			myParameters = theParameters;
			return this;
		}

		public Builder setReporter(String theReporter) {
			myReporter = theReporter;
			return this;
		}

		public EvaluateMeasuresPluralParams build() {
			return new EvaluateMeasuresPluralParams(this);
		}
	}
}
