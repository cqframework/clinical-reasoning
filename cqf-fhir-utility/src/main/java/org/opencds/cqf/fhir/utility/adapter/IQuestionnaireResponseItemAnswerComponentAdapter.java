package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;

public interface IQuestionnaireResponseItemAnswerComponentAdapter extends IAdapter<IBase> {

    boolean hasValue();

    IBase getValue();

    void setValue(IBaseDatatype value);

    boolean hasItem();

    List<IQuestionnaireResponseItemComponentAdapter> getItem();

    void setItem(List<IQuestionnaireResponseItemComponentAdapter> items);
}
