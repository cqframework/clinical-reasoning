package org.opencds.cqf.fhir.utility.adapter;

import static java.util.Collections.singletonList;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;

public interface ICodeableConceptAdapter extends IAdapter<IBase> {

    boolean hasCoding();

    List<ICodingAdapter> getCoding();

    boolean hasCoding(String code);

    default ICodingAdapter getCodingFirstRep() {
        List<ICodingAdapter> codings = getCoding();
        return codings.isEmpty() ? null : codings.get(0);
    }

    ICodeableConceptAdapter addCoding(String system, String code, String display);

    default ICodeableConceptAdapter setCoding(List<IBaseCoding> coding) {
        setValue("coding", coding);
        return this;
    }

    default ICodeableConceptAdapter addCoding(IBaseCoding coding) {
        setValue("coding", singletonList(coding));
        return this;
    }
}
