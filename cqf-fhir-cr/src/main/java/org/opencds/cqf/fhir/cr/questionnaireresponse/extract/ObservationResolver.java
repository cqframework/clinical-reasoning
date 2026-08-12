package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static java.util.Collections.singletonList;
import static org.opencds.cqf.fhir.utility.Resources.newBaseForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.decimalTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.integerTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.referenceTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.stringTypeForVersion;
import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newDateTimeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;

public class ObservationResolver {
    // Observation-based extraction -
    // http://build.fhir.org/ig/HL7/sdc/extraction.html#observation-based-extraction

    public IBaseResource resolve(
            ExtractRequest request,
            IQuestionnaireResponseItemAnswerComponentAdapter answerAdapter,
            IQuestionnaireItemComponentAdapter itemAdapter,
            String linkId,
            IBaseReference subject,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            IBaseExtension<?, ?> categoryExt) {
        var questionnaireResponseAdapter = request.getQuestionnaireResponseAdapter();
        var authoredDate = (questionnaireResponseAdapter.hasAuthored()
                        ? questionnaireResponseAdapter.getAuthored().toInstant()
                        : Instant.now())
                .toString();
        var obs = request.getAdapterFactory()
                .createObservation((IBaseResource) newBaseForVersion("Observation", request.getFhirVersion()))
                .setBasedOn(questionnaireResponseAdapter.getBasedOn())
                .setPartOf(questionnaireResponseAdapter.getPartOf())
                .setDerivedFrom(singletonList(referenceTypeForVersion(
                        request.getFhirVersion(), (IAnyResource) questionnaireResponseAdapter.get())))
                .setStatus("final")
                .setCategory(singletonList((ICompositeType) (categoryExt == null
                                ? request.getAdapterFactory()
                                        .createCodeableConcept(
                                                newBaseForVersion("CodeableConcept", request.getFhirVersion()))
                                        .addCoding(
                                                Constants.SDC_OBSERVATION_CATEGORY, Constants.SDC_CATEGORY_SURVEY, null)
                                : request.getAdapterFactory().createCodeableConcept(categoryExt.getValue()))
                        .get()))
                .setCode((ICompositeType) request.getAdapterFactory()
                        .createCodeableConcept(newBaseForVersion("CodeableConcept", request.getFhirVersion()))
                        .setCoding(new ArrayList<>(questionnaireCodeMap.get(linkId)))
                        .get())
                .setSubject(subject)
                .setEncounter(questionnaireResponseAdapter.getEncounter())
                .setEffective(authoredDate)
                .setIssued(authoredDate)
                .setPerformer(singletonList(questionnaireResponseAdapter.getAuthor()))
                .setValue(getAnswerValue(request, answerAdapter, itemAdapter));
        obs.setId(request.getExtractId() + "." + linkId);
        var linkIdExtension = obs.addExtension();
        linkIdExtension.setUrl("http://hl7.org/fhir/uv/sdc/StructureDefinition/derivedFromLinkId");
        var innerLinkIdExtension = ((IBaseHasExtensions) linkIdExtension).addExtension();
        innerLinkIdExtension.setUrl("text");
        innerLinkIdExtension.setValue(stringTypeForVersion(request.getFhirVersion(), linkId));
        return obs.get();
    }

    protected IBaseDatatype getAnswerValue(
            ExtractRequest request,
            IQuestionnaireResponseItemAnswerComponentAdapter answerAdapter,
            IQuestionnaireItemComponentAdapter itemAdapter) {
        IBaseDatatype value;
        switch (answerAdapter.getValue().fhirType()) {
            case "Coding":
                value = (IBaseDatatype) request.getAdapterFactory()
                        .createCodeableConcept(newBaseForVersion("CodeableConcept", request.getFhirVersion()))
                        .addCoding((IBaseCoding) answerAdapter.getValue())
                        .get();
                break;
            case "date":
                //noinspection unchecked
                value = newDateTimeType(
                        request.getFhirVersion(), ((IPrimitiveType<Date>) answerAdapter.getValue()).getValue());
                break;
            case "decimal", "integer":
                if (itemAdapter != null && itemAdapter.hasExtension(Constants.QUESTIONNAIRE_UNIT)) {
                    value = getQuantity(request, answerAdapter, itemAdapter);
                } else {
                    value = (IBaseDatatype) answerAdapter.getValue();
                }
                break;
            default:
                value = (IBaseDatatype) answerAdapter.getValue();
        }
        return value;
    }

    protected ICompositeType getQuantity(
            ExtractRequest request,
            IQuestionnaireResponseItemAnswerComponentAdapter answer,
            IQuestionnaireItemComponentAdapter item) {
        var unit = request.getAdapterFactory()
                .createBase(item.getExtensionByUrl(Constants.QUESTIONNAIRE_UNIT).getValue());
        var quantity = request.getAdapterFactory().createBase(newBaseForVersion("Quantity", request.getFhirVersion()));
        quantity.setValue("unit", unit.resolvePath("display"));
        quantity.setValue("system", unit.resolvePath("system"));
        quantity.setValue("code", unit.resolvePath("code"));
        var value = answer.getValue();
        if (value.fhirType().equals("DecimalType")) {
            //noinspection unchecked
            quantity.setValue(
                    "value",
                    decimalTypeForVersion(request.getFhirVersion(), ((IPrimitiveType<BigDecimal>) answer).getValue()));
        }
        if (value.fhirType().equals("IntegerType")) {
            //noinspection unchecked
            quantity.setValue(
                    "value",
                    integerTypeForVersion(request.getFhirVersion(), ((IPrimitiveType<Integer>) answer).getValue()));
        }
        return (ICompositeType) quantity.get();
    }
}
