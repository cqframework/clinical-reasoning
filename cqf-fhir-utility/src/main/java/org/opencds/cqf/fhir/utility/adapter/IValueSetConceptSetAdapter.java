package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;

public interface IValueSetConceptSetAdapter extends IAdapter<IBase> {
    boolean hasConcept();

    List<IValueSetConceptReferenceAdapter> getConcept();

    boolean hasSystem();

    String getSystem();
}
