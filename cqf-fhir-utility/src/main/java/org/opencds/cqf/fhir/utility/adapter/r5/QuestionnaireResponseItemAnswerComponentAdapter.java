package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

public class QuestionnaireResponseItemAnswerComponentAdapter extends BaseAdapter
        implements IQuestionnaireResponseItemAnswerComponentAdapter {

    private final QuestionnaireResponseItemAnswerComponent answer;

    protected QuestionnaireResponseItemAnswerComponentAdapter(IBase answer) {
        super(FhirVersionEnum.R5, answer);
        if (!(answer instanceof QuestionnaireResponseItemAnswerComponent)) {
            throw new IllegalArgumentException(
                    "object passed as answer argument is not a QuestionnaireResponseItemAnswerComponent data type");
        }
        this.answer = (QuestionnaireResponseItemAnswerComponent) answer;
    }

    @Override
    public QuestionnaireResponseItemAnswerComponent get() {
        return answer;
    }

    @Override
    public boolean hasValue() {
        return answer.hasValue();
    }

    @Override
    public IBase getValue() {
        return answer.getValue();
    }

    @Override
    public void setValue(IBaseDatatype value) {
        answer.setValue((DataType) value);
    }

    @Override
    public boolean hasItem() {
        return answer.hasItem();
    }

    @Override
    public List<IQuestionnaireResponseItemComponentAdapter> getItem() {
        return answer.getItem().stream()
                .map(adapterFactory::createQuestionnaireResponseItem)
                .toList();
    }

    @Override
    public void setItem(List<IQuestionnaireResponseItemComponentAdapter> items) {
        answer.setItem(items.stream()
                .map(IAdapter::get)
                .map(QuestionnaireResponseItemComponent.class::cast)
                .toList());
    }
}
