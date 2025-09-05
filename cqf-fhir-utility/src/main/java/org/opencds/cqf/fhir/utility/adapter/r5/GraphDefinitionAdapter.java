package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.GraphDefinition;
import org.hl7.fhir.r5.model.UsageContext;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;

public class GraphDefinitionAdapter extends ResourceAdapter implements IGraphDefinitionAdapter {
    public GraphDefinitionAdapter(IDomainResource graphDefinition) {
        super(graphDefinition);
        if (!(graphDefinition instanceof GraphDefinition)) {
            throw new IllegalArgumentException(
                    "resource passed as graphDefinition argument is not a GraphDefinition resource");
        }
    }

    public GraphDefinitionAdapter(GraphDefinition graphDefinition) {
        super(graphDefinition);
    }

    protected GraphDefinition getGraphDefinition() {
        return (GraphDefinition) resource;
    }

    @Override
    public GraphDefinition get() {
        return getGraphDefinition();
    }

    @Override
    public GraphDefinition copy() {
        return get().copy();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        /*
           extension[cpg-relatedArtifact].reference
        */

        get().getExtensionsByUrl(Constants.CPG_RELATED_ARTIFACT).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(Expression::hasReference)
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));

        get().getExtensionsByUrl(Constants.ARTIFACT_RELATED_ARTIFACT).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(Expression::hasReference)
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));

        return references;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<UsageContext> getUseContext() {
        return getGraphDefinition().getUseContext();
    }

    @Override
    public String getStatus() {
        return getGraphDefinition().getStatus().toCode();
    }

    @Override
    public void setStatus(String status) {
        getGraphDefinition().setStatus(PublicationStatus.fromCode(status));
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifactsOfType(String codeString) {
        return List.of();
    }

    @Override
    public List<IBaseBackboneElement> getBackBoneElements() {
        return List.of();
    }

    @Override
    public List<IBaseBackboneElement> getNode() {
        return List.of();
    }
}
