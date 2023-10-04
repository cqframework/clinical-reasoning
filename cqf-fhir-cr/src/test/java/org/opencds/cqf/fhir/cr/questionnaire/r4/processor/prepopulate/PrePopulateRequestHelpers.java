package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;

public class PrePopulateRequestHelpers {
    protected static PrePopulateRequest withPrePopulateRequest(LibraryEngine theLibraryEngine) {
        final Questionnaire questionnaire = new Questionnaire();
        final CanonicalType type = new CanonicalType("url");
        final Extension extension = new Extension(Constants.CQF_LIBRARY, type);
        questionnaire.addExtension(extension);
        final String patientId = "patientId";
        final IBaseParameters parameters = new Parameters();
        final IBaseBundle bundle = new Bundle();
        return new PrePopulateRequest(
            questionnaire,
            patientId,
            parameters,
            bundle,
            theLibraryEngine
        );
    }
}
