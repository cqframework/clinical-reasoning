package org.opencds.cqf.fhir.cr.measure;

public class CareGapsProperties {
  private boolean myThreadedCareGapsEnabled = true;
  /**
   * Implements the reporter element of the
   * <a href= "https://www.hl7.org/fhir/measurereport.html">MeasureReport</a> FHIR Resource. This is
   * required by the <a href=
   * "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm">DEQMIndividualMeasureReportProfile</a>
   * profile found in the <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci
   * DEQM FHIR Implementation Guide</a>.
   **/
  private String myCareGapsReporter;
  /**
   * Implements the author element of the
   * <a href= "http://www.hl7.org/fhir/composition.html">Composition</a> FHIR Resource. This is
   * required by the <a href=
   * "http://build.fhir.org/ig/HL7/davinci-deqm/StructureDefinition-gaps-composition-deqm.html">DEQMGapsInCareCompositionProfile</a>
   * profile found in the <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci
   * DEQM FHIR Implementation Guide</a>.
   **/
  private String myFhirBaseUrl;
  private String myCareGapsCompositionSectionAuthor;

  // care gaps
  public boolean getThreadedCareGapsEnabled() {
    return myThreadedCareGapsEnabled;
  }

  public void setThreadedCareGapsEnabled(boolean theThreadedCareGapsEnabled) {
    myThreadedCareGapsEnabled = theThreadedCareGapsEnabled;
  }

  public boolean isThreadedCareGapsEnabled() {
    return myThreadedCareGapsEnabled;
  }

  public String getMyFhirBaseUrl() {
    return myFhirBaseUrl;
  }

  public void setMyFhirBaseUrl(String theFhirBaseUrl) {
    myFhirBaseUrl = theFhirBaseUrl;
  }

  public String getCareGapsReporter() {
    return myCareGapsReporter;
  }

  public void setCareGapsReporter(String theCareGapsReporter) {
    myCareGapsReporter = theCareGapsReporter;
  }

  public String getCareGapsCompositionSectionAuthor() {
    return myCareGapsCompositionSectionAuthor;
  }

  public void setCareGapsCompositionSectionAuthor(String theCareGapsCompositionSectionAuthor) {
    myCareGapsCompositionSectionAuthor = theCareGapsCompositionSectionAuthor;
  }
}
