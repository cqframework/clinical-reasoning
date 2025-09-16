package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

public class QuestionnaireResponseItemComponentAdapter extends BaseAdapter
        implements IQuestionnaireResponseItemComponentAdapter {

    private final QuestionnaireResponseItemComponent item;

    public QuestionnaireResponseItemComponentAdapter(IBase item) {
        super(FhirVersionEnum.R5, item);
        if (!(item instanceof QuestionnaireResponseItemComponent)) {
            throw new IllegalArgumentException(
                    "object passed as item argument is not a QuestionnaireResponseItemComponent data type");
        }
        this.item = (QuestionnaireResponseItemComponent) item;
    }

    @Override
    public QuestionnaireResponseItemComponent get() {
        return item;
    }

    @Override
    public String getLinkId() {
        return item.getLinkId();
    }

    @Override
    public boolean hasDefinition() {
        return item.hasDefinition();
    }

    @Override
    public String getDefinition() {
        return item.getDefinition();
    }

    @Override
    public boolean hasItem() {
        return item.hasItem();
    }

    @Override
    public List<IQuestionnaireResponseItemComponentAdapter> getItem() {
        return item.getItem().stream()
                .map(adapterFactory::createQuestionnaireResponseItem)
                .toList();
    }

    @Override
    public void setItem(List<? extends IItemComponentAdapter> items) {
        item.setItem(items.stream()
                .map(IAdapter::get)
                .map(QuestionnaireResponseItemComponent.class::cast)
                .toList());
    }

    @Override
    public void addItem(IItemComponentAdapter item) {
        this.item.addItem((QuestionnaireResponseItemComponent) item.get());
    }

    @Override
    public void addItems(List<IItemComponentAdapter> items) {
        items.stream()
                .map(IAdapter::get)
                .map(QuestionnaireResponseItemComponent.class::cast)
                .forEach(this.item::addItem);
    }

    @Override
    public boolean hasAnswer() {
        return item.hasAnswer();
    }

    @Override
    public List<IQuestionnaireResponseItemAnswerComponentAdapter> getAnswer() {
        return item.getAnswer().stream()
                .map(adapterFactory::createQuestionnaireResponseItemAnswer)
                .collect(Collectors.toList());
    }

    @Override
    public void setAnswer(List<IQuestionnaireResponseItemAnswerComponentAdapter> answers) {
        item.setAnswer(answers.stream()
                .map(IAdapter::get)
                .filter(QuestionnaireResponseItemAnswerComponent.class::isInstance)
                .map(QuestionnaireResponseItemAnswerComponent.class::cast)
                .toList());
    }

    @Override
    public IQuestionnaireResponseItemAnswerComponentAdapter createAnswer(IBaseDatatype value) {
        return adapterFactory.createQuestionnaireResponseItemAnswer(
                new QuestionnaireResponseItemAnswerComponent().setValue((DataType) value));
    }
}
