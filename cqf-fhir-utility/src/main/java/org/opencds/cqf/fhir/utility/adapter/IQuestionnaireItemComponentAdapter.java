package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseDatatype;

public interface IQuestionnaireItemComponentAdapter extends IItemComponentAdapter {

    List<IBaseCoding> getCode();

    String getType();

    boolean isGroupItem();

    boolean getRepeats();

    boolean hasInitial();

    List<? extends IBaseDatatype> getInitial();

    IQuestionnaireResponseItemComponentAdapter newResponseItem();
}
