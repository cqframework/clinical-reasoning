package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Enumerations.CompositionStatus;

public class CompositionBuilder<T extends IDomainResource> extends BaseDomainResourceBuilder<CompositionBuilder<T>, T> {

    private String status;
    private String title;
    private CodeableConceptSettings type;
    private String subject;
    private String author;
    private String custodian;

    private Date date = new Date();

    public CompositionBuilder(Class<T> resourceClass) {
        super(resourceClass);
    }

    public CompositionBuilder(Class<T> resourceClass, String id) {
        super(resourceClass, id);
    }

    public CompositionBuilder(
            Class<T> resourceClass,
            String id,
            CodeableConceptSettings type,
            String status,
            String author,
            String title) {
        this(resourceClass, id);
        checkNotNull(type);
        checkNotNull(status);
        checkNotNull(author);
        checkNotNull(title);

        this.type = type;
        this.status = status;
        this.author = author;
        this.title = title;
    }

    public CompositionBuilder<T> withStatus(String status) {
        checkNotNull(status);

        this.status = status;

        return this;
    }

    public CompositionBuilder<T> withTitle(String title) {
        checkNotNull(title);

        this.title = title;

        return this;
    }

    public CompositionBuilder<T> withType(CodeableConceptSettings type) {
        checkNotNull(type);

        this.type = type;

        return this;
    }

    public CompositionBuilder<T> withDate(Date date) {
        checkNotNull(date);

        this.date = date;

        return this;
    }

    public CompositionBuilder<T> withSubject(String subject) {
        this.subject = ensurePatientReference(subject);

        return this;
    }

    public CompositionBuilder<T> withAuthor(String author) {
        checkNotNull(author);
        checkArgument(author.startsWith("Practitioner")
                || author.startsWith("PractitionerRole")
                || author.startsWith("Device")
                || author.startsWith("Patient")
                || author.startsWith("RelatedPerson")
                || author.startsWith("Organization"));
        this.author = author;

        return this;
    }

    public CompositionBuilder<T> withCustodian(String custodian) {
        this.custodian = ensureOrganizationReference(custodian);

        return this;
    }

    @Override
    public T build() {
        checkNotNull(type);
        checkNotNull(status);
        checkNotNull(author);
        checkNotNull(title);

        checkArgument(
                !type.getCodingSettings().isEmpty() && type.getCodingSettings().size() == 1);

        return super.build();
    }

    private CodingSettings getTypeSetting() {
        return type.getCodingSettingsArray()[0];
    }

    @Override
    protected void initializeDstu3(T resource) {
        super.initializeDstu3(resource);
        org.hl7.fhir.dstu3.model.Composition composition = (org.hl7.fhir.dstu3.model.Composition) resource;

        composition
                .setDate(date)
                .setIdentifier(new org.hl7.fhir.dstu3.model.Identifier()
                        .setSystem(getIdentifier().getKey())
                        .setValue(getIdentifier().getValue()))
                .setStatus(org.hl7.fhir.dstu3.model.Composition.CompositionStatus.valueOf(status))
                .setSubject(new org.hl7.fhir.dstu3.model.Reference(subject))
                .setTitle(title)
                .setType(new org.hl7.fhir.dstu3.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.dstu3.model.Coding()
                                .setSystem(getTypeSetting().getSystem())
                                .setCode(getTypeSetting().getCode())
                                .setDisplay(getTypeSetting().getDisplay())))
                .addAuthor(new org.hl7.fhir.dstu3.model.Reference(author))
                .setCustodian(new org.hl7.fhir.dstu3.model.Reference(custodian));
    }

    @Override
    protected void initializeR4(T resource) {
        super.initializeR4(resource);
        org.hl7.fhir.r4.model.Composition composition = (org.hl7.fhir.r4.model.Composition) resource;

        composition
                .setDate(date)
                .setIdentifier(new org.hl7.fhir.r4.model.Identifier()
                        .setSystem(getIdentifier().getKey())
                        .setValue(getIdentifier().getValue()))
                .setStatus(org.hl7.fhir.r4.model.Composition.CompositionStatus.valueOf(status))
                .setSubject(new org.hl7.fhir.r4.model.Reference(subject))
                .setTitle(title)
                .setType(new org.hl7.fhir.r4.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.r4.model.Coding()
                                .setSystem(getTypeSetting().getSystem())
                                .setCode(getTypeSetting().getCode())
                                .setDisplay(getTypeSetting().getDisplay())))
                .addAuthor(new org.hl7.fhir.r4.model.Reference(author))
                .setCustodian(new org.hl7.fhir.r4.model.Reference(custodian));
    }

    @Override
    protected void initializeR5(T resource) {
        super.initializeR5(resource);
        org.hl7.fhir.r5.model.Composition composition = (org.hl7.fhir.r5.model.Composition) resource;
        List<org.hl7.fhir.r5.model.Identifier> r5Identifiers = new ArrayList<>();
        org.hl7.fhir.r5.model.Identifier r5Identifier = new org.hl7.fhir.r5.model.Identifier();
        r5Identifier
                .setSystem(getIdentifier().getKey())
                .setValue(getIdentifier().getValue());
        r5Identifiers.add(r5Identifier);
        List<org.hl7.fhir.r5.model.Reference> r5References = new ArrayList<>();
        org.hl7.fhir.r5.model.Reference r5Reference = new org.hl7.fhir.r5.model.Reference(subject);
        r5References.add(r5Reference);
        composition
                .setDate(date)
                .setIdentifier(r5Identifiers)
                .setStatus(CompositionStatus.valueOf(status))
                .setSubject(r5References)
                .setTitle(title)
                .setType(new org.hl7.fhir.r5.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.r5.model.Coding()
                                .setSystem(getTypeSetting().getSystem())
                                .setCode(getTypeSetting().getCode())
                                .setDisplay(getTypeSetting().getDisplay())))
                .addAuthor(new org.hl7.fhir.r5.model.Reference(author))
                .setCustodian(new org.hl7.fhir.r5.model.Reference(custodian));
    }
}
