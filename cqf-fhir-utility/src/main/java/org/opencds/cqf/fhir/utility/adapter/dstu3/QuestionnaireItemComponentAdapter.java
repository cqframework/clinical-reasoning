package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

public class QuestionnaireItemComponentAdapter extends BaseAdapter implements IQuestionnaireItemComponentAdapter {

    private final QuestionnaireItemComponent item;

    public QuestionnaireItemComponentAdapter(IBase item) {
        super(FhirVersionEnum.DSTU3, item);
        if (!(item instanceof QuestionnaireItemComponent)) {
            throw new IllegalArgumentException(
                    "object passed as item argument is not a QuestionnaireItemComponent data type");
        }
        this.item = (QuestionnaireItemComponent) item;
    }

    @Override
    public QuestionnaireItemComponent get() {
        return item;
    }

    @Override
    public String getLinkId() {
        return item.getLinkId();
    }

    @Override
    public List<IBaseCoding> getCode() {
        return item.getCode().stream().map(IBaseCoding.class::cast).toList();
    }

    @Override
    public String getType() {
        return item.getType().toCode();
    }

    @Override
    public boolean isGroupItem() {
        return item.getType().equals(QuestionnaireItemType.GROUP);
    }

    @Override
    public boolean getRepeats() {
        return item.getRepeats();
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
    public List<IQuestionnaireItemComponentAdapter> getItem() {
        return item.getItem().stream()
                .map(adapterFactory::createQuestionnaireItem)
                .toList();
    }

    @Override
    public void setItem(List<? extends IItemComponentAdapter> items) {
        item.setItem(items.stream()
                .map(IAdapter::get)
                .map(QuestionnaireItemComponent.class::cast)
                .toList());
    }

    @Override
    public void addItem(IItemComponentAdapter item) {
        this.item.addItem((QuestionnaireItemComponent) item.get());
    }

    @Override
    public void addItems(List<IItemComponentAdapter> items) {
        items.stream()
                .map(IAdapter::get)
                .map(QuestionnaireItemComponent.class::cast)
                .forEach(this.item::addItem);
    }

    @Override
    public boolean hasInitial() {
        return item.hasInitial();
    }

    @Override
    public List<Type> getInitial() {
        return List.of(item.getInitial());
    }

    @Override
    public IQuestionnaireResponseItemComponentAdapter newResponseItem() {
        return adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent()
                .setLinkId(item.getLinkId())
                .setDefinitionElement(item.getDefinitionElement())
                .setTextElement(item.getTextElement()));
    }
}
