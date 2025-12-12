package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.GraphDefinition;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r5.model.UsageContext;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;

public class GraphDefinitionAdapter extends ResourceAdapter implements IGraphDefinitionAdapter<GraphDefinition> {
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

        extractRelatedArtifactReferences(get(), referenceSource, references);

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
    public <EXTENSION extends IBaseExtension<?, ?>> Class<EXTENSION> extensionClass() {
        return (Class<EXTENSION>) Extension.class;
    }

    @Override
    public <ARTIFACT extends IBaseDatatype> String getReferenceFromArtifact(ARTIFACT artifact) {
        String ref = null;
        if (artifact instanceof RelatedArtifact relArtifact) {
            ref = relArtifact.getResource();

            // fallback; if no canonical url, we'll get it from the resource reference
            if (isBlank(ref)) {
                Reference reference = relArtifact.getResourceReference();
                if (reference != null) {
                    ref = reference.getReference();
                }
            }
        }

        return ref;
    }

    @Override
    public <RA extends IBaseDatatype> void validateRelatedArtifact(RA relatedArtifact, List<String> errors) {
        if (relatedArtifact instanceof RelatedArtifact relArtifact) {
            if (relArtifact.getType() != RelatedArtifactType.DEPENDSON) {
                errors.add(String.format(
                        "Expected RelatedArtifact of type \"depends-on\"; found \"%s\"",
                        relArtifact.getType().name()));
            }
        } else {
            errors.add(String.format(
                    "Expected RelatedArtifact; found %s",
                    relatedArtifact == null ? "null" : relatedArtifact.fhirType()));
        }
    }
}
