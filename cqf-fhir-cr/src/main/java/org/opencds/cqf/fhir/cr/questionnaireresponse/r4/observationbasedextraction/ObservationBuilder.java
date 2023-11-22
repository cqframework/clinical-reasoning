package org.opencds.cqf.fhir.cr.questionnaireresponse.r4.observationbasedextraction;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import javax.annotation.Nonnull;
import java.util.List;

public class ObservationBuilder {
    private String id;
    private List<Reference> basedOn;
    private List<Reference> partOf;
    private ObservationStatus status;
    private List<CodeableConcept> category;
    private CodeableConcept code;
    private Reference encounter;
    private Reference subject;
    private Type effective;
    private InstantType issuedElement;
    private List<Reference> performer;
    private Type value;
    private List<Reference> derivedFrom;
    private Extension extension;

    @Nonnull
    ObservationBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Nonnull
    ObservationBuilder basedOn(List<Reference> basedOn) {
        this.basedOn = basedOn;
        return this;
    }

    @Nonnull
    ObservationBuilder partOf(List<Reference> partOf) {
        this.partOf = partOf;
        return this;
    }

    @Nonnull
    ObservationBuilder status(ObservationStatus status) {
        this.status = status;
        return this;
    }

    @Nonnull
    ObservationBuilder category(List<CodeableConcept> category) {
        this.category = category;
        return this;
    }

    @Nonnull
    ObservationBuilder code(CodeableConcept code) {
        this.code = code;
        return this;
    }

    @Nonnull
    ObservationBuilder subject(Reference subject) {
        this.subject = subject;
        return this;
    }

    @Nonnull
    ObservationBuilder encounter(Reference encounter) {
        this.encounter = encounter;
        return this;
    }

    @Nonnull
    ObservationBuilder effective(Type effective) {
        this.effective = effective;
        return this;
    }

    @Nonnull
    ObservationBuilder issuedElement(InstantType issued) {
        this.issuedElement = issued;
        return this;
    }

    @Nonnull
    ObservationBuilder performer(List<Reference> performer) {
        this.performer = performer;
        return this;
    }

    @Nonnull
    ObservationBuilder value(Type value) {
        this.value = value;
        return this;
    }

    @Nonnull
    ObservationBuilder derived(List<Reference> derivedFrom) {
        this.derivedFrom = derivedFrom;
        return this;
    }

    @Nonnull
    ObservationBuilder extension(Extension extension) {
        this.extension = extension;
        return this;
    }

    @Nonnull
    Observation build() {
        final Observation observation = new Observation();
        observation.setId(this.id);
        observation.setBasedOn(this.basedOn);
        observation.setPartOf(this.partOf);
        observation.setStatus(this.status);
        observation.setCategory(this.category);
        observation.setCode(this.code);
        observation.setEncounter(this.encounter);
        observation.setSubject(this.subject);
        observation.setEffective(this.effective);
        observation.setIssuedElement(this.issuedElement);
        observation.setPerformer(this.performer);
        observation.setValue(this.value);
        observation.setDerivedFrom(this.derivedFrom);
        observation.setExtension(List.of(this.extension));
        return observation;
    }
}
