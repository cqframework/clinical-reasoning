package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static java.util.Collections.singletonList;
import static org.opencds.cqf.fhir.utility.Resources.newBaseForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.decimalTypeForVersion;
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
import org.hl7.fhir.instance.model.api.IBase;
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
        var fhirVersion = request.getFhirVersion();
        var questionnaireResponseAdapter = request.getQuestionnaireResponseAdapter();
        var authoredDate = (questionnaireResponseAdapter.hasAuthored()
                        ? questionnaireResponseAdapter.getAuthored().toInstant()
                        : Instant.now())
                .toString();
        var category = (ICompositeType) (categoryExt == null
                        ? request.getAdapterFactory()
                                .createCodeableConcept(newCodeableConcept(request))
                                .addCoding(Constants.SDC_OBSERVATION_CATEGORY, Constants.SDC_CATEGORY_SURVEY, null)
                        : request.getAdapterFactory().createCodeableConcept(categoryExt.getValue()))
                .get();
        var code = (ICompositeType) request.getAdapterFactory()
                .createCodeableConcept(newCodeableConcept(request))
                .setCoding(new ArrayList<>(questionnaireCodeMap.get(linkId)))
                .get();
        var obs = request.getAdapterFactory()
                .createObservation((IBaseResource) newBaseForVersion("Observation", fhirVersion))
                .setBasedOn(questionnaireResponseAdapter.getBasedOn())
                .setPartOf(questionnaireResponseAdapter.getPartOf())
                .setDerivedFrom(singletonList(
                        referenceTypeForVersion(fhirVersion, (IAnyResource) questionnaireResponseAdapter.get())))
                .setStatus("final")
                .setCategory(singletonList(category))
                .setCode(code)
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
        innerLinkIdExtension.setValue(stringTypeForVersion(fhirVersion, linkId));
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
                        .createCodeableConcept(newCodeableConcept(request))
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
                    value = getQuantity(
                            request,
                            answerAdapter,
                            itemAdapter
                                    .getExtensionByUrl(Constants.QUESTIONNAIRE_UNIT)
                                    .getValue());
                } else {
                    value = (IBaseDatatype) answerAdapter.getValue();
                }
                break;
            default:
                value = (IBaseDatatype) answerAdapter.getValue();
        }
        return value;
    }

    protected IBaseDatatype getQuantity(
            ExtractRequest request, IQuestionnaireResponseItemAnswerComponentAdapter answer, IBaseDatatype unitCoding) {
        var unit = request.getAdapterFactory().createCoding(unitCoding);
        var quantity = request.getAdapterFactory().createBase(newBaseForVersion("Quantity", request.getFhirVersion()));
        quantity.setValue("code", unit.getCodeType());
        quantity.setValue("unit", stringTypeForVersion(request.getFhirVersion(), unit.getDisplay()));
        quantity.setValue("system", stringTypeForVersion(request.getFhirVersion(), unit.getSystem()));
        var value = answer.getValue();
        if (value.fhirType().equals("decimal")) {
            //noinspection unchecked
            quantity.setValue(
                    "value",
                    decimalTypeForVersion(request.getFhirVersion(), ((IPrimitiveType<BigDecimal>) value).getValue()));
        }
        if (value.fhirType().equals("integer")) {
            //noinspection unchecked
            quantity.setValue(
                    "value",
                    decimalTypeForVersion(
                            request.getFhirVersion(), new BigDecimal(((IPrimitiveType<Integer>) value).getValue())));
        }
        return (IBaseDatatype) quantity.get();
    }

    protected IBase newCodeableConcept(ExtractRequest request) {
        return newBaseForVersion("CodeableConcept", request.getFhirVersion());
    }
}
