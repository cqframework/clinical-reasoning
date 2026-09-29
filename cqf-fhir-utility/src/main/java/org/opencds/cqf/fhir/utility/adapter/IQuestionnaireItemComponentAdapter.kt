package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.ICompositeType;

public interface IQuestionnaireItemComponentAdapter extends IItemComponentAdapter {

    IQuestionnaireItemComponentAdapter setLinkId(String linkId);

    IQuestionnaireItemComponentAdapter setDefinition(String definition);

    void addItems(List<IQuestionnaireItemComponentAdapter> items);

    List<IBaseCoding> getCode();

    String getText();

    IQuestionnaireItemComponentAdapter setText(String text);

    String getType();

    IQuestionnaireItemComponentAdapter setType(String type);

    boolean isGroupItem();

    boolean isChoiceItem();

    boolean getRequired();

    IQuestionnaireItemComponentAdapter setRequired(boolean required);

    boolean getRepeats();

    IQuestionnaireItemComponentAdapter setRepeats(boolean repeats);

    void addAnswerOption(ICodingAdapter option);

    boolean hasInitial();

    List<? extends IBaseDatatype> getInitial();

    IQuestionnaireResponseItemComponentAdapter newResponseItem();

    ICompositeType newExpression(String language, String expression);

    default ICompositeType newExpression(String expression) {
        return newExpression("text/cql-expression", expression);
    }
}
