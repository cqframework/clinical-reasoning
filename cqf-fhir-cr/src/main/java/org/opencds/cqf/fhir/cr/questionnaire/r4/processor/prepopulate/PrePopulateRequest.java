package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.opencds.cqf.fhir.cql.LibraryEngine;

public class PrePopulateRequest {
    private final Questionnaire questionnaire;
    private final String patientId;
    private final IBaseParameters parameters;
    private final IBaseBundle bundle;
    private final LibraryEngine libraryEngine;

    public PrePopulateRequest(
            Questionnaire questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        this.questionnaire = questionnaire;
        this.patientId = patientId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
    }

    public IBaseBundle getBundle() {
        return bundle;
    }

    public IBaseParameters getParameters() {
        return parameters;
    }

    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public String getPatientId() {
        return patientId;
    }
}
