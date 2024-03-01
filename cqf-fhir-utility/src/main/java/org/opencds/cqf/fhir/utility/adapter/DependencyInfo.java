package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;

public class DependencyInfo implements IDependencyInfo {
    // TODO: Need for figuring out how to determine which package the dependency is in.
    private String referenceSource;
    private String reference;
    private String referencePackageId;
    private List<? extends IBaseExtension<?, ?>> extensionList;

    public DependencyInfo() {}

    public DependencyInfo(
            String referenceSource, String reference, List<? extends IBaseExtension<?, ?>> extensionList) {
        this.referenceSource = referenceSource;
        this.reference = reference;
        this.extensionList = extensionList;
    }

    public String getReferenceSource() {
        return this.referenceSource;
    }

    public List<? extends IBaseExtension<?, ?>> getExtension() {
        return this.extensionList;
    }

    public void setReferenceSource(String referenceSource) {
        this.referenceSource = referenceSource;
    }

    public String getReference() {
        return this.reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReferencePackageId() {
        return this.referencePackageId;
    }

    public void setReferencePackageId(String referencePackageId) {
        this.referencePackageId = referencePackageId;
    }

    public static IDependencyInfo convertRelatedArtifact(ICompositeType ra, String source) {
        if (ra instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
            return new DependencyInfo(
                    source,
                    ((org.hl7.fhir.dstu3.model.RelatedArtifact) ra)
                            .getResource()
                            .getReference(),
                    ((org.hl7.fhir.dstu3.model.RelatedArtifact) ra).getExtension());
        } else if (ra instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
            return new DependencyInfo(
                    source,
                    ((org.hl7.fhir.r4.model.RelatedArtifact) ra).getResource(),
                    ((org.hl7.fhir.r4.model.RelatedArtifact) ra).getExtension());
        } else if (ra instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
            return new DependencyInfo(
                    source,
                    ((org.hl7.fhir.r5.model.RelatedArtifact) ra).getResource(),
                    ((org.hl7.fhir.r5.model.RelatedArtifact) ra).getExtension());
        } else {
            throw new UnprocessableEntityException("A valid RelatedArtifact object must be provided");
        }
    }
}
