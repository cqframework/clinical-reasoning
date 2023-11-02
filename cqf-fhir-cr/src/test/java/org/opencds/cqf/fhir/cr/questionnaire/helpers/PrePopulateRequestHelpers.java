package org.opencds.cqf.fhir.cr.questionnaire.helpers;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class PrePopulateRequestHelpers {
    public static final String PATIENT_ID = "patientId";

    public static PrePopulateRequest withPrePopulateRequest(LibraryEngine libraryEngine) {
        final IBaseParameters parameters = new Parameters();
        final IBaseBundle bundle = new Bundle();
        return new PrePopulateRequest(PATIENT_ID, parameters, bundle, libraryEngine);
    }
}
