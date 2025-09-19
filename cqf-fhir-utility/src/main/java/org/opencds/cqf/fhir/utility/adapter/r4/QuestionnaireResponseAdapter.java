package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

public class QuestionnaireResponseAdapter extends ResourceAdapter implements IQuestionnaireResponseAdapter {

    public QuestionnaireResponseAdapter(IDomainResource questionnaireResponse) {
        super(questionnaireResponse);
        if (!(questionnaireResponse instanceof QuestionnaireResponse)) {
            throw new IllegalArgumentException(
                    "resource passed as questionnaire argument is not a QuestionnaireResponse resource");
        }
    }

    public QuestionnaireResponseAdapter(QuestionnaireResponse questionnaireResponse) {
        super(questionnaireResponse);
    }

    protected QuestionnaireResponse getQuestionnaireResponse() {
        return (QuestionnaireResponse) resource;
    }

    @Override
    public QuestionnaireResponse get() {
        return getQuestionnaireResponse();
    }

    @Override
    public IQuestionnaireResponseAdapter setId(String id) {
        get().setId(id);
        return this;
    }

    @Override
    public IQuestionnaireResponseAdapter setQuestionnaire(String canonical) {
        get().setQuestionnaire(canonical);
        return this;
    }

    @Override
    public IQuestionnaireResponseAdapter setSubject(IIdType subject) {
        get().setSubject(new Reference(subject));
        return this;
    }

    @Override
    public IQuestionnaireResponseAdapter setAuthored(Date date) {
        get().setAuthored(date);
        return this;
    }

    @Override
    public IQuestionnaireResponseAdapter setStatus(String status) {
        get().setStatus(QuestionnaireResponseStatus.fromCode(status));
        return this;
    }

    @Override
    public boolean hasItem() {
        return getQuestionnaireResponse().hasItem();
    }

    @Override
    public List<IQuestionnaireResponseItemComponentAdapter> getItem() {
        return getQuestionnaireResponse().getItem().stream()
                .map(adapterFactory::createQuestionnaireResponseItem)
                .toList();
    }

    @Override
    public void setItem(List<IQuestionnaireResponseItemComponentAdapter> items) {
        getQuestionnaireResponse()
                .setItem(items.stream()
                        .map(IAdapter::get)
                        .map(QuestionnaireResponseItemComponent.class::cast)
                        .collect(Collectors.toList()));
    }

    @Override
    public void addItem(IQuestionnaireResponseItemComponentAdapter item) {
        getQuestionnaireResponse().addItem((QuestionnaireResponseItemComponent) item.get());
    }

    @Override
    public void addItems(List<IQuestionnaireResponseItemComponentAdapter> items) {
        items.stream()
                .map(IAdapter::get)
                .map(QuestionnaireResponseItemComponent.class::cast)
                .forEach(item -> getQuestionnaireResponse().addItem(item));
    }
}
