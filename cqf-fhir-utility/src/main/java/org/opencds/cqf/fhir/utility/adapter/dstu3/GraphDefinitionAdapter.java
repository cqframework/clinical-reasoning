package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.GraphDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.UsageContext;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
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
         *  extension[cpg-relatedArtifact].resource
         */
        extractRelatedArtifactReferences(referenceSource, references);

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

    @Override
    public <ARTIFACT extends IBaseDatatype> String getReferenceFromArtifact(ARTIFACT artifact) {
        String url = null;
        if (artifact instanceof RelatedArtifact relArt) {
            Reference ref = relArt.getResource();
            if (ref != null) {
                url = ref.getReference();
            }
        }
        return url;
    }

    @Override
    public <RA extends IBaseDatatype> boolean canProcessRelatedArtifact(RA relatedArtifact) {
        if (relatedArtifact instanceof RelatedArtifact relArtifact) {
            return relArtifact.getType() == RelatedArtifactType.DEPENDSON;
        }

        return false;
    }
}
