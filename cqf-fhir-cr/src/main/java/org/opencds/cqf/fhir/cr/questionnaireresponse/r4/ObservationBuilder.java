package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ObservationBuilder {
    ProcessorHelper processorHelper;
    Observation build(
        QuestionnaireResponseItemAnswerComponent answer,
        String linkId,
        QuestionnaireResponse questionnaireResponse,
        Reference subject,
        Map<String, List<Coding>> questionnaireCodeMap
    ) {
        // Observation-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#observation-based-extraction
        final Observation observation = new Observation();
        observation.setId(processorHelper.getExtractId(questionnaireResponse) + "." + linkId);
        observation.setBasedOn(questionnaireResponse.getBasedOn());
        observation.setPartOf(questionnaireResponse.getPartOf());
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCategory(Collections.singletonList(new CodeableConcept().addCoding(getCategoryCoding())));
        observation.setCode(new CodeableConcept().setCoding(questionnaireCodeMap.get(linkId)));
        observation.setSubject(subject);
        observation.setEncounter(questionnaireResponse.getEncounter());
        observation.setEffective(getAuthoredDate(questionnaireResponse));
        observation.setIssuedElement(new InstantType(getAuthoredDate(questionnaireResponse)));
        observation.setPerformer(Collections.singletonList(questionnaireResponse.getAuthor()));
        observation.setValue(getValue(answer));
        observation.setDerivedFrom(Collections.singletonList(new Reference(questionnaireResponse)));
        observation.addExtension(getLinkExtension(linkId));
        return observation;
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

    DateTimeType getAuthoredDate(QuestionnaireResponse questionnaireResponse) {
        return new DateTimeType((questionnaireResponse.hasAuthored()
            ? questionnaireResponse.getAuthored().toInstant()
            : Instant.now())
            .toString());
    }

    Coding getCategoryCoding() {
        final Coding qrCategoryCoding = new Coding();
        qrCategoryCoding.setCode("survey");
        qrCategoryCoding.setSystem("http://hl7.org/fhir/observation-category");
        return qrCategoryCoding;
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

}
