package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.dstu3.model.DetectedIssue.DetectedIssueStatus;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DetectedIssue.DetectedIssueEvidenceComponent;
import org.hl7.fhir.r5.model.DetectedIssue;

public class DetectedIssueBuilder<T extends IDomainResource> extends DomainResourceBuilder<DetectedIssueBuilder<T>, T> {

    private String status;
    private CodeableConceptSettings c;
    private String patient;
    private List<String> evidenceDetails;

    public DetectedIssueBuilder(Class<T> resourceClass) {
        super(resourceClass);
    }

    public DetectedIssueBuilder(Class<T> resourceClass, String id) {
        super(resourceClass, id);
    }

    public DetectedIssueBuilder(Class<T> resourceClass, String id, String status, String evidenceDetail) {
        super(resourceClass, id);
        checkNotNull(status);
        checkNotNull(evidenceDetail);

        this.status = status;
        addEvidenceDetail(evidenceDetail);
    }

    private void addEvidenceDetail(String evidenceDetail) {
        if (evidenceDetails == null) {
            evidenceDetails = new ArrayList<>();
        }

        evidenceDetails.add(evidenceDetail);
    }

    private List<String> getEvidenceDetails() {
        if (evidenceDetails == null) {
            return Collections.emptyList();
        }

        return evidenceDetails;
    }

    public DetectedIssueBuilder<T> withStatus(String status) {
        this.status = status;

        return this;
    }

    public DetectedIssueBuilder<T> withCode(CodeableConceptSettings theCode) {
        checkNotNull(theCode);

        c = theCode;

        return this;
    }

    public DetectedIssueBuilder<T> withPatient(String thePatient) {
        patient = ensurePatientReference(thePatient);

        return this;
    }

    public DetectedIssueBuilder<T> withEvidenceDetail(String evidenceDetail) {
        checkNotNull(evidenceDetail);

        addEvidenceDetail(evidenceDetail);

        return this;
    }

    @Override
    public T build() {
        checkNotNull(status);
        checkNotNull(evidenceDetails);
        checkArgument(!evidenceDetails.isEmpty());

        return super.build();
    }

    private CodingSettings getCodeSetting() {
        return c.getCodingSettingsArray()[0];
    }

    @Override
    protected void initializeDstu3(T resource) {
        super.initializeDstu3(resource);
        org.hl7.fhir.dstu3.model.DetectedIssue detectedIssue = (org.hl7.fhir.dstu3.model.DetectedIssue) resource;

        detectedIssue
                .setIdentifier(new org.hl7.fhir.dstu3.model.Identifier()
                        .setSystem(getIdentifier().getKey())
                        .setValue(getIdentifier().getValue()))
                .setPatient(new org.hl7.fhir.dstu3.model.Reference(patient))
                .setStatus(DetectedIssueStatus.valueOf(status));
        getEvidenceDetails().forEach(detectedIssue::setReference);
    }

    @Override
    protected void initializeR4(T resource) {
        super.initializeR4(resource);
        org.hl7.fhir.r4.model.DetectedIssue detectedIssue = (org.hl7.fhir.r4.model.DetectedIssue) resource;

        List<org.hl7.fhir.r4.model.Identifier> identifier = new ArrayList<>();
        identifier.add(new org.hl7.fhir.r4.model.Identifier()
                .setSystem(getIdentifier().getKey())
                .setValue(getIdentifier().getValue()));

        detectedIssue
                .setIdentifier(identifier)
                .setPatient(new org.hl7.fhir.r4.model.Reference(patient))
                .setStatus(org.hl7.fhir.r4.model.DetectedIssue.DetectedIssueStatus.valueOf(status))
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(getCodeSetting().getSystem())
                                .setCode(getCodeSetting().getCode())
                                .setDisplay(getCodeSetting().getDisplay())));
        getEvidenceDetails()
                .forEach(evidence -> detectedIssue.addEvidence(
                        new DetectedIssueEvidenceComponent().addDetail(new org.hl7.fhir.r4.model.Reference(evidence))));
    }

    @Override
    protected void initializeR5(T resource) {
        super.initializeR5(resource);
        org.hl7.fhir.r5.model.DetectedIssue detectedIssue = (org.hl7.fhir.r5.model.DetectedIssue) resource;

        List<org.hl7.fhir.r5.model.Identifier> identifier = new ArrayList<>();
        identifier.add(new org.hl7.fhir.r5.model.Identifier()
                .setSystem(getIdentifier().getKey())
                .setValue(getIdentifier().getValue()));

        detectedIssue
                .setIdentifier(identifier)
                .setStatus(DetectedIssue.DetectedIssueStatus.valueOf(status))
                .setCode(new org.hl7.fhir.r5.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.r5.model.Coding()
                                .setSystem(getCodeSetting().getSystem())
                                .setCode(getCodeSetting().getCode())
                                .setDisplay(getCodeSetting().getDisplay())));
        getEvidenceDetails()
                .forEach(evidence -> detectedIssue.addEvidence(
                        new org.hl7.fhir.r5.model.DetectedIssue.DetectedIssueEvidenceComponent()
                                .addDetail(new org.hl7.fhir.r5.model.Reference(evidence))));
    }
}
