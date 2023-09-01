package org.opencds.cqf.fhir.cr.measure.constant;

import java.sql.Date;

public class MeasureReportConstants {
  private MeasureReportConstants() {}

  public static final String MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/measure-improvement-notation";
  public static final String MEASUREREPORT_MEASURE_POPULATION_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/measure-population";
  public static final String MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION =
      "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData";
  public static final String MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL =
      "http://hl7.org/fhir/us/davinci-deqm/SearchParameter/measurereport-supplemental-data";
  public static final String MEASUREREPORT_PRODUCT_LINE_EXT_URL =
      "http://hl7.org/fhir/us/cqframework/cqfmeasures/StructureDefinition/cqfm-productLine";
  public static final String MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION = "0.1.0";
  public static final Date MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE =
      Date.valueOf("2022-07-20"); // issue without local date?
  public static final String COUNTRY_CODING_SYSTEM_CODE = "urn:iso:std:iso:3166";
  public static final String US_COUNTRY_CODE = "US";
  public static final String US_COUNTRY_DISPLAY = "United States of America";
}
