package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseDatatype;

public interface IQuestionnaireResponseItemComponentAdapter extends IItemComponentAdapter {

    boolean hasAnswer();

    List<IQuestionnaireResponseItemAnswerComponentAdapter> getAnswer();

    void setAnswer(List<IQuestionnaireResponseItemAnswerComponentAdapter> answers);

    IQuestionnaireResponseItemAnswerComponentAdapter createAnswer(IBaseDatatype value);
}
