package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Enumerations.CompositionStatus;

public class CompositionBuilder<T extends IDomainResource> extends DomainResourceBuilder<CompositionBuilder<T>, T> {

    private String myStatus;
    private String myTitle;
    private CodeableConceptSettings myType;
    private String mySubject;
    private String myAuthor;
    private String myCustodian;

    private Date myDate = new Date();

    public CompositionBuilder(Class<T> theResourceClass) {
        super(theResourceClass);
    }

    public CompositionBuilder(Class<T> theResourceClass, String theId) {
        super(theResourceClass, theId);
    }

    public CompositionBuilder(
            Class<T> theResourceClass,
            String theId,
            CodeableConceptSettings theType,
            String theStatus,
            String theAuthor,
            String theTitle) {
        this(theResourceClass, theId);
        checkNotNull(theType, theStatus, theAuthor, theTitle);

        myType = theType;
        myStatus = theStatus;
        myAuthor = theAuthor;
        myTitle = theTitle;
    }

    public CompositionBuilder<T> withStatus(String theStatus) {
        checkNotNull(theStatus);

        myStatus = theStatus;

        return this;
    }

    public CompositionBuilder<T> withTitle(String theTitle) {
        checkNotNull(theTitle);

        myTitle = theTitle;

        return this;
    }

    public CompositionBuilder<T> withType(CodeableConceptSettings theType) {
        checkNotNull(theType);

        myType = theType;

        return this;
    }

    public CompositionBuilder<T> withDate(Date theDate) {
        checkNotNull(theDate);

        myDate = theDate;

        return this;
    }

    public CompositionBuilder<T> withSubject(String theSubject) {
        mySubject = ensurePatientReference(theSubject);

        return this;
    }

    public CompositionBuilder<T> withAuthor(String theAuthor) {
        checkNotNull(theAuthor);
        checkArgument(theAuthor.startsWith("Practitioner")
                || theAuthor.startsWith("PractitionerRole")
                || theAuthor.startsWith("Device")
                || theAuthor.startsWith("Patient")
                || theAuthor.startsWith("RelatedPerson")
                || theAuthor.startsWith("Organization"));
        myAuthor = theAuthor;

        return this;
    }

    public CompositionBuilder<T> withCustodian(String theCustodian) {
        myCustodian = ensureOrganizationReference(theCustodian);

        return this;
    }

    @Override
    public T build() {
        checkNotNull(myType, myStatus, myAuthor, myTitle);
        checkArgument(!myType.getCodingSettings().isEmpty()
                && myType.getCodingSettings().size() == 1);

        return super.build();
    }

    private CodingSettings getTypeSetting() {
        return myType.getCodingSettingsArray()[0];
    }

    @Override
    protected void initializeDstu3(T theResource) {
        super.initializeDstu3(theResource);
        org.hl7.fhir.dstu3.model.Composition composition = (org.hl7.fhir.dstu3.model.Composition) theResource;

        composition
                .setDate(myDate)
                .setIdentifier(new org.hl7.fhir.dstu3.model.Identifier()
                        .setSystem(getIdentifier().getKey())
                        .setValue(getIdentifier().getValue()))
                .setStatus(org.hl7.fhir.dstu3.model.Composition.CompositionStatus.valueOf(myStatus))
                .setSubject(new org.hl7.fhir.dstu3.model.Reference(mySubject))
                .setTitle(myTitle)
                .setType(new org.hl7.fhir.dstu3.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.dstu3.model.Coding()
                                .setSystem(getTypeSetting().getSystem())
                                .setCode(getTypeSetting().getCode())
                                .setDisplay(getTypeSetting().getDisplay())))
                .addAuthor(new org.hl7.fhir.dstu3.model.Reference(myAuthor))
                .setCustodian(new org.hl7.fhir.dstu3.model.Reference(myCustodian));
    }

    @Override
    protected void initializeR4(T theResource) {
        super.initializeR4(theResource);
        org.hl7.fhir.r4.model.Composition composition = (org.hl7.fhir.r4.model.Composition) theResource;

        composition
                .setDate(myDate)
                .setIdentifier(new org.hl7.fhir.r4.model.Identifier()
                        .setSystem(getIdentifier().getKey())
                        .setValue(getIdentifier().getValue()))
                .setStatus(org.hl7.fhir.r4.model.Composition.CompositionStatus.valueOf(myStatus))
                .setSubject(new org.hl7.fhir.r4.model.Reference(mySubject))
                .setTitle(myTitle)
                .setType(new org.hl7.fhir.r4.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.r4.model.Coding()
                                .setSystem(getTypeSetting().getSystem())
                                .setCode(getTypeSetting().getCode())
                                .setDisplay(getTypeSetting().getDisplay())))
                .addAuthor(new org.hl7.fhir.r4.model.Reference(myAuthor))
                .setCustodian(new org.hl7.fhir.r4.model.Reference(myCustodian));
    }

    @Override
    protected void initializeR5(T theResource) {
        super.initializeR5(theResource);
        org.hl7.fhir.r5.model.Composition composition = (org.hl7.fhir.r5.model.Composition) theResource;
        List<org.hl7.fhir.r5.model.Identifier> r5Identifiers = new ArrayList<>();
        org.hl7.fhir.r5.model.Identifier r5Identifier = new org.hl7.fhir.r5.model.Identifier();
        r5Identifier
                .setSystem(getIdentifier().getKey())
                .setValue(getIdentifier().getValue());
        r5Identifiers.add(r5Identifier);
        List<org.hl7.fhir.r5.model.Reference> r5References = new ArrayList<>();
        org.hl7.fhir.r5.model.Reference r5Reference = new org.hl7.fhir.r5.model.Reference(mySubject);
        r5References.add(r5Reference);
        composition
                .setDate(myDate)
                .setIdentifier(r5Identifiers)
                .setStatus(CompositionStatus.valueOf(myStatus))
                .setSubject(r5References)
                .setTitle(myTitle)
                .setType(new org.hl7.fhir.r5.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.r5.model.Coding()
                                .setSystem(getTypeSetting().getSystem())
                                .setCode(getTypeSetting().getCode())
                                .setDisplay(getTypeSetting().getDisplay())))
                .addAuthor(new org.hl7.fhir.r5.model.Reference(myAuthor))
                .setCustodian(new org.hl7.fhir.r5.model.Reference(myCustodian));
    }
}
