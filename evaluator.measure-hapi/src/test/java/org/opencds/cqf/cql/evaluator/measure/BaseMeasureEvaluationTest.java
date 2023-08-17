package org.opencds.cqf.cql.evaluator.measure;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;

public abstract class BaseMeasureEvaluationTest {

  protected static final String FHIR_NS_URI = "http://hl7.org/fhir";
  protected static final String OMB_CATEGORY_RACE_BLACK = "2054-5";
  protected static final String BLACK_OR_AFRICAN_AMERICAN = "Black or African American";
  protected static final String URL_SYSTEM_RACE = "urn:oid:2.16.840.1.113883.6.238";
  protected static final String OMB_CATEGORY = "ombCategory";
  protected static final String EXT_URL_US_CORE_RACE =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race";

  protected Interval measurementPeriod(String periodStart, String periodEnd) {
    ZoneOffset offset = ZonedDateTime.now().getOffset();

    DateTime start = new DateTime(periodStart, offset);
    DateTime end = new DateTime(periodEnd, offset);

    return new Interval(start, true, end, true);
  }

  public String cql_with_date() {
    return library_header() + measurement_period_date() + patient_context();
  }

  public String cql_with_dateTime() {
    return library_header() + measurement_period_dateTime() + patient_context();
  }

  public String library_header() {
    return String.format(
        "library Test version '1.0.0'\n\nusing FHIR version '%1$s'\ninclude FHIRHelpers version '%1$s'\n\n",
        getFhirVersion());
  }

  public String measurement_period_date() {
    return "parameter \"Measurement Period\" Interval<Date> default Interval[@2019-01-01, @2019-12-31]\n\n";
  }

  public String measurement_period_dateTime() {
    return "parameter \"Measurement Period\" Interval<DateTime> default Interval[@2019-01-01T00:00:00.0, @2020-01-01T00:00:00.0)\n\n";
  }

  public String patient_context() {
    return "context Patient\n";
  }

  public abstract String getFhirVersion();

  protected String sde_race() {
    return "define \"SDE Race\":\n" + "  (flatten (\n" + "    Patient.extension Extension\n"
        + "      where Extension.url = 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-race'\n"
        + "        return Extension.extension\n" + "  )) E\n" + "    where E.url = 'ombCategory'\n"
        + "      or E.url = 'detailed'\n" + "    return E.value as Coding\n\n";
  }

  protected static String formatMsg(List<CqlCompilerException> translationErrs) {
    StringBuilder msg = new StringBuilder();
    msg.append("Translation failed due to errors:");
    for (CqlCompilerException error : translationErrs) {
      TrackBack tb = error.getLocator();
      String lines = tb == null ? "[n/a]"
          : String.format("[%d:%d, %d:%d]", tb.getStartLine(), tb.getStartChar(), tb.getEndLine(),
              tb.getEndChar());
      msg.append(String.format("%s %s%n", lines, error.getMessage()));
    }
    return msg.toString();
  }

}
