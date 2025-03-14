package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class RequestActionAdapter implements IRequestActionAdapter {

    private final RequestOrchestrationActionComponent action;
    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private final ModelResolver modelResolver;

    public RequestActionAdapter(IBaseBackboneElement action) {
        if (!(action instanceof RequestOrchestrationActionComponent)) {
            throw new IllegalArgumentException(
                    "element passed as action argument is not a RequestOrchestrationActionComponent Element");
        }
        this.action = (RequestOrchestrationActionComponent) action;
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
    }

    @Override
    public RequestOrchestrationActionComponent get() {
        return action;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
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
