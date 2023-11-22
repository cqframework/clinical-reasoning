package org.opencds.cqf.fhir.cr.questionnaireresponse.r4.observationbasedextraction;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.cr.questionnaireresponse.common.ProcessorHelper;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class ObservationFactory {
    ProcessorHelper processorHelper;
    static final ObservationStatus status = Observation.ObservationStatus.FINAL;

    final Observation makeObservation(
        QuestionnaireResponseItemAnswerComponent answer,
        String linkId,
        QuestionnaireResponse questionnaireResponse,
        Reference subject,
        Map<String, List<Coding>> questionnaireCodeMap
    ) {
        final String id = getId(questionnaireResponse, linkId);
        final List<Reference> basedOn = getBasedOn(questionnaireResponse);
        final List<Reference> partOf = getPartOf(questionnaireResponse);
        final List<CodeableConcept> category = getCategory();
        final CodeableConcept code = getCode(questionnaireCodeMap, linkId);
        final Reference encounter = getEncounter(questionnaireResponse);
        final Type effective = getEffective(questionnaireResponse);
        final InstantType issued = getIssued(questionnaireResponse);
        final List<Reference> performer = getPerformer(questionnaireResponse);
        final Type value = getValue(answer);
        final List<Reference> derived = getDerivedFrom(questionnaireResponse);
        final Extension extension = getLinkExtension(linkId);
        return new ObservationBuilder()
            .id(id)
            .basedOn(basedOn)
            .partOf(partOf)
            .performer(performer)
            .status(status)
            .category(category)
            .code(code)
            .subject(subject)
            .encounter(encounter)
            .effective(effective)
            .issuedElement(issued)
            .performer(performer)
            .value(value)
            .derived(derived)
            .extension(extension)
            .build();
    }

    final String getId(QuestionnaireResponse questionnaireResponse, String linkId) {
        return processorHelper.getExtractId(questionnaireResponse) + "." + linkId;
    }

    final List<Reference> getBasedOn(QuestionnaireResponse questionnaireResponse) {
        return questionnaireResponse.getBasedOn();
    }

    final List<Reference> getPartOf(QuestionnaireResponse questionnaireResponse) {
        return questionnaireResponse.getPartOf();
    }

    Type getValue(QuestionnaireResponseItemAnswerComponent answer) {
        switch (answer.getValue().fhirType()) {
            case "Coding":
                return new CodeableConcept().addCoding(answer.getValueCoding());
            case "date":
                return new DateTimeType(((DateType) answer.getValue()).getValue());
            default:
                return answer.getValue();
        }
    }

    Extension getLinkExtension(String linkId) {
        final Extension linkIdExtension = new Extension();
        linkIdExtension.setUrl("http://hl7.org/fhir/uv/sdc/StructureDefinition/derivedFromLinkId");
        final Extension innerLinkIdExtension = new Extension();
        innerLinkIdExtension.setUrl("text");
        innerLinkIdExtension.setValue(new StringType(linkId));
        linkIdExtension.setExtension(Collections.singletonList(innerLinkIdExtension));
        return linkIdExtension;
    }

    CodeableConcept getCode(Map<String, List<Coding>> questionnaireCodeMap, String linkId) {
        return new CodeableConcept().setCoding(questionnaireCodeMap.get(linkId));
    }

    List<CodeableConcept> getCategory() {
        return Collections.singletonList(new CodeableConcept().addCoding(getCategoryCoding()));
    }

    Coding getCategoryCoding() {
        final Coding qrCategoryCoding = new Coding();
        qrCategoryCoding.setCode("survey");
        qrCategoryCoding.setSystem("http://hl7.org/fhir/observation-category");
        return qrCategoryCoding;
    }

    Reference getEncounter(QuestionnaireResponse questionnaireResponse) {
        return questionnaireResponse.getEncounter();
    }
    Type getEffective(QuestionnaireResponse questionnaireResponse) {
        return getAuthoredDate(questionnaireResponse);
    }

    InstantType getIssued(QuestionnaireResponse questionnaireResponse) {
        return new InstantType(getAuthoredDate(questionnaireResponse));
    }

    DateTimeType getAuthoredDate(QuestionnaireResponse questionnaireResponse) {
        return new DateTimeType((questionnaireResponse.hasAuthored()
            ? questionnaireResponse.getAuthored().toInstant()
            : Instant.now())
            .toString());
    }

    List<Reference> getPerformer(QuestionnaireResponse questionnaireResponse) {
        return Collections.singletonList(questionnaireResponse.getAuthor());
    }

    List<Reference> getDerivedFrom(QuestionnaireResponse questionnaireResponse) {
        return Collections.singletonList(new Reference(questionnaireResponse));
    }
}
