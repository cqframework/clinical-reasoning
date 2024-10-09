package org.opencds.cqf.fhir.cr.measure;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;

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
    private String careGapsCompositionSectionAuthor;

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

    public void validateRequiredProperties() {
        checkArgument(
                !Strings.isNullOrEmpty(getCareGapsReporter()),
                "Setting care-gaps properties.care_gaps_reporter setting is required for the $care-gaps operation.");
        checkArgument(
                !Strings.isNullOrEmpty(getCareGapsCompositionSectionAuthor()),
                "Setting care-gaps properties.care_gaps_composition_section_author is required for the $care-gaps operation.");
    }
}
