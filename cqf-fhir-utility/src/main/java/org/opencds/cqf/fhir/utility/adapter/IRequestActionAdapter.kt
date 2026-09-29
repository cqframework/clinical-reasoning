package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.ICompositeType;

public interface IRequestActionAdapter extends IAdapter<IBase> {

    String getId();

    IRequestActionAdapter setId(String id);

    boolean hasTitle();

    String getTitle();

    IRequestActionAdapter setTitle(String title);

    boolean hasDescription();

    String getDescription();

    IRequestActionAdapter setDescription(String description);

    boolean hasTextEquivalent();

    String getTextEquivalent();

    IRequestActionAdapter setTextEquivalent(String text);

    boolean hasPriority();

    String getPriority();

    IRequestActionAdapter setPriority(String priority);

    boolean hasCode();

    ICodeableConceptAdapter getCode();

    IRequestActionAdapter setCode(ICodeableConceptAdapter code);

    boolean hasDocumentation();

    <T extends ICompositeType & IBaseHasExtensions> List<T> getDocumentation();

    <T extends ICompositeType & IBaseHasExtensions> IRequestActionAdapter setDocumentation(List<T> documentation);

    boolean hasCondition();

    <T extends IBaseBackboneElement> List<T> getCondition();

    void addCondition(IBaseBackboneElement condition);

    boolean hasRelatedAction();

    <T extends IBaseBackboneElement> List<T> getRelatedAction();

    void addRelatedAction(IBaseBackboneElement relatedAction);

    boolean hasTiming();

    IBaseDatatype getTiming();

    IRequestActionAdapter setTiming(IBaseDatatype timing);

    boolean hasType();

    ICodeableConceptAdapter getType();

    IRequestActionAdapter setType(ICodeableConceptAdapter type);

    boolean hasSelectionBehavior();

    String getSelectionBehavior();

    IRequestActionAdapter setSelectionBehavior(String behavior);

    boolean hasResource();

    IBaseReference getResource();

    IRequestActionAdapter setResource(IBaseReference resource);

    boolean hasAction();

    List<IRequestActionAdapter> getAction();

    void addAction(IBaseBackboneElement action);

    IRequestActionAdapter setAction(List<IRequestActionAdapter> actions);
}
