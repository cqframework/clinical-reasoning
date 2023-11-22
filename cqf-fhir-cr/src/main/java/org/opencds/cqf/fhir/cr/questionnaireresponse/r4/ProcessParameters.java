package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import java.util.List;
import java.util.Map;

public class ProcessParameters {
    private QuestionnaireResponseItemComponent item;
    private final QuestionnaireResponse questionnaireResponse;
    private final List<IBaseResource> resources;
    private Reference subject;
    private final Map<String, List<Coding>> questionnaireCodeMap;

    public ProcessParameters(QuestionnaireResponseItemComponent item,
        QuestionnaireResponse questionnaireResponse, List<IBaseResource> resources,
        Reference subject, Map<String, List<Coding>> questionnaireCodeMap) {
        this.item = item;
        this.questionnaireResponse = questionnaireResponse;
        this.resources = resources;
        this.subject = subject;
        this.questionnaireCodeMap = questionnaireCodeMap;
    }

    public QuestionnaireResponseItemComponent getItem() {
        return item;
    }

    public QuestionnaireResponse getQuestionnaireResponse() {
        return questionnaireResponse;
    }

    public List<IBaseResource> getResources() {
        return resources;
    }

    public void addToResources(IBaseResource resource) {
        this.resources.add(resource);
    }

    public void setSubject(Reference reference) {
        this.subject = reference;
    }
    public void setItem(QuestionnaireResponseItemComponent responseItem) {
        this.item = responseItem;
    }
    public Reference getSubject() {
        return subject;
    }
    public Map<String, List<Coding>> getQuestionnaireCodeMap() {
        return questionnaireCodeMap;
    }
}
