package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public interface IValueSetConceptSetAdapter extends IAdapter<IBaseBackboneElement> {
    boolean hasConcept();

    List<IValueSetConceptReferenceAdapter> getConcept();

    String getSystem();
}
