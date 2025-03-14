package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class RequestActionAdapter implements IRequestActionAdapter {

    private final RequestGroupActionComponent action;
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();
    private final ModelResolver modelResolver;

    public RequestActionAdapter(IBaseBackboneElement action) {
        if (!(action instanceof RequestGroupActionComponent)) {
            throw new IllegalArgumentException(
                    "element passed as action argument is not a RequestGroupActionComponent Element");
        }
        this.action = (RequestGroupActionComponent) action;
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
    }

    @Override
    public RequestGroupActionComponent get() {
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
        return new CodeableConceptAdapter(new CodeableConcept().addCoding(get().getType()));
    }

    @Override
    public boolean hasPriority() {
        return false;
    }

    @Override
    public String getPriority() {
        return null;
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
