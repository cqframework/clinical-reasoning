package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;

public interface ICodeableConceptAdapter extends IAdapter<IBase> {

    boolean hasCoding();

    List<ICodingAdapter> getCoding();

    boolean hasCoding(String code);
}
