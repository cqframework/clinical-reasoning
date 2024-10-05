package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public class ItemPair extends ImmutablePair<IBaseBackboneElement, IBaseBackboneElement> {

    public ItemPair(IBaseBackboneElement item, IBaseBackboneElement responseItem) {
        super(item, responseItem);
    }

    public IBaseBackboneElement getItem() {
        return left;
    }

    public IBaseBackboneElement getResponseItem() {
        return right;
    }
}
