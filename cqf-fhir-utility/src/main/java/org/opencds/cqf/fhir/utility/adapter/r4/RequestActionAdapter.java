package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;

public class RequestActionAdapter extends BaseAdapter implements IRequestActionAdapter {

    private final RequestGroupActionComponent action;

    public RequestActionAdapter(IBase action) {
        super(FhirVersionEnum.R4, action);
        if (!(action instanceof RequestGroupActionComponent)) {
            throw new IllegalArgumentException(
                    "element passed as action argument is not a RequestGroupActionComponent Element");
        }
        this.action = (RequestGroupActionComponent) action;
    }

    @Override
    public RequestGroupActionComponent get() {
        return action;
    }

    @Override
    public String getId() {
        return get().getId();
    }

    @Override
    public boolean hasTitle() {
        return get().hasTitle();
    }

    @Override
    public String getTitle() {
        return get().getTitle();
    }

    @Override
    public boolean hasDescription() {
        return get().hasDescription();
    }

    @Override
    public String getDescription() {
        return get().getDescription();
    }

    @Override
    public boolean hasType() {
        return get().hasType();
    }

    @Override
    public ICodeableConceptAdapter getType() {
        return new CodeableConceptAdapter(get().getType());
    }

    @Override
    public boolean hasPriority() {
        return get().hasPriority();
    }

    @Override
    public String getPriority() {
        return get().getPriority().toCode();
    }

    @Override
    public boolean hasDocumentation() {
        return get().hasDocumentation();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getDocumentation() {
        return get().getDocumentation();
    }

    @Override
    public boolean hasSelectionBehavior() {
        return get().hasSelectionBehavior();
    }

    @Override
    public String getSelectionBehavior() {
        return get().getSelectionBehavior().toCode();
    }

    @Override
    public boolean hasResource() {
        return get().hasResource();
    }

    @Override
    public Reference getResource() {
        return get().getResource();
    }

    @Override
    public boolean hasAction() {
        return get().hasAction();
    }

    @Override
    public List<IRequestActionAdapter> getAction() {
        return get().getAction().stream().map(RequestActionAdapter::new).collect(Collectors.toUnmodifiableList());
    }
}
