package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.ICompositeType;

public interface IRequestActionAdapter extends IAdapter<IBaseBackboneElement> {

    String getId();

    boolean hasTitle();

    String getTitle();

    boolean hasDescription();

    String getDescription();

    boolean hasType();

    ICodeableConceptAdapter getType();

    boolean hasPriority();

    String getPriority();

    boolean hasDocumentation();

    <T extends ICompositeType & IBaseHasExtensions> List<T> getDocumentation();

    boolean hasSelectionBehavior();

    String getSelectionBehavior();

    boolean hasResource();

    IBaseReference getResource();

    boolean hasAction();

    List<IRequestActionAdapter> getAction();
}
