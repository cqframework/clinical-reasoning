package org.opencds.cqf.fhir.cr.measure;

public class CareGapsProperties {
    private String careGapsReporter;
    /**
     * Implements the author element of the
     * <a href= "http://www.hl7.org/fhir/composition.html">Composition</a> FHIR Resource. This is
     * required by the <a href=
     * "http://build.fhir.org/ig/HL7/davinci-deqm/StructureDefinition-gaps-composition-deqm.html">DEQMGapsInCareCompositionProfile</a>
     * profile found in the <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci
     * DEQM FHIR Implementation Guide</a>.
     **/
    private String fhirBaseUrl;

    private String careGapsCompositionSectionAuthor;

    public String getMyFhirBaseUrl() {
        return fhirBaseUrl;
    }

    public void setMyFhirBaseUrl(String fhirBaseUrl) {
        this.fhirBaseUrl = fhirBaseUrl;
    }

    public String getCareGapsReporter() {
        return careGapsReporter;
    }

    public void setCareGapsReporter(String careGapsReporter) {
        this.careGapsReporter = careGapsReporter;
    }

    public String getCareGapsCompositionSectionAuthor() {
        return careGapsCompositionSectionAuthor;
    }

    public void setCareGapsCompositionSectionAuthor(String careGapsCompositionSectionAuthor) {
        this.careGapsCompositionSectionAuthor = careGapsCompositionSectionAuthor;
    }
}
