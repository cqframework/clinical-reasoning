package org.opencds.cqf.fhir.cr.questionnaireresponse.extract.r4;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class ObservationResolver {
    public IBaseResource resolve(
            ExtractRequest request,
            IBaseBackboneElement baseAnswer,
            IBaseBackboneElement baseItem,
            String linkId,
            IBaseReference subject,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            IBaseExtension<?, ?> categoryExt) {
        var questionnaireResponse = (QuestionnaireResponse) request.getQuestionnaireResponse();
        var answer = (QuestionnaireResponseItemAnswerComponent) baseAnswer;
        var item = (QuestionnaireItemComponent) baseItem;
        var obs = new Observation();
        obs.setId(request.getExtractId() + "." + linkId);
        obs.setBasedOn(questionnaireResponse.getBasedOn());
        obs.setPartOf(questionnaireResponse.getPartOf());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        var qrCategoryCode = categoryExt == null
                ? new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(Constants.SDC_OBSERVATION_CATEGORY)
                                .setCode(Constants.SDC_CATEGORY_SURVEY))
                : (CodeableConcept) categoryExt.getValue();
        obs.setCategory(Collections.singletonList(qrCategoryCode));

        obs.setCode(new CodeableConcept()
                .setCoding(questionnaireCodeMap.get(linkId).stream()
                        .map(c -> (Coding) c)
                        .collect(Collectors.toList())));
        obs.setSubject((Reference) subject);
        // obs.setFocus();
        obs.setEncounter(questionnaireResponse.getEncounter());
        var authoredDate = new DateTimeType((questionnaireResponse.hasAuthored()
                        ? questionnaireResponse.getAuthored().toInstant()
                        : Instant.now())
                .toString());
        obs.setEffective(authoredDate);
        obs.setIssuedElement(new InstantType(authoredDate));
        obs.setPerformer(Collections.singletonList(questionnaireResponse.getAuthor()));

        switch (answer.getValue().fhirType()) {
            case "Coding":
                obs.setValue(new CodeableConcept().addCoding(answer.getValueCoding()));
                break;
            case "date":
                obs.setValue(new DateTimeType(((DateType) answer.getValue()).getValue()));
                break;
            case "DecimalType":
            case "IntegerType":
                if (item.hasExtension(Constants.QUESTIONNAIRE_UNIT)) {
                    var unit = (Coding) item.getExtensionByUrl(Constants.QUESTIONNAIRE_UNIT).getValue();
                    var quantity = new Quantity().setUnit(unit.getDisplay()).setSystem(unit.getSystem()).setCode(unit.getCode());
                    if (answer.hasValueDecimalType()) {
                        quantity.setValueElement(answer.getValueDecimalType());
                    }
                    if (answer.hasValueIntegerType()) {
                        quantity.setValue(answer.getValueIntegerType().getValue());
                    }
                    obs.setValue(quantity);
                } else {
                    obs.setValue(answer.getValue());
                }
            default:
                obs.setValue(answer.getValue());
        }
        obs.setDerivedFrom(Collections.singletonList(new Reference(questionnaireResponse)));

        var linkIdExtension = new Extension();
        linkIdExtension.setUrl("http://hl7.org/fhir/uv/sdc/StructureDefinition/derivedFromLinkId");
        var innerLinkIdExtension = new Extension();
        innerLinkIdExtension.setUrl("text");
        innerLinkIdExtension.setValue(new StringType(linkId));
        linkIdExtension.setExtension(Collections.singletonList(innerLinkIdExtension));
        obs.addExtension(linkIdExtension);
        return obs;
    }
}
