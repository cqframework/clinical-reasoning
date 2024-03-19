package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public interface KnowledgeArtifactAdapter extends ResourceAdapter {

    IDomainResource get();

    IDomainResource copy();

    default IIdType getId() {
        return this.get().getIdElement();
    }

    default void setId(IIdType id) {
        this.get().setId(id);
    }

    String getName();

    void setName(String name);

    boolean hasUrl();

    String getUrl();

    void setUrl(String url);

    boolean hasVersion();

    String getVersion();

    void setVersion(String version);

    List<IDependencyInfo> getDependencies();

    Date getApprovalDate();

    void setApprovalDate(Date approvalDate);

    Date getDate();

    void setDate(Date date);

    void setDateElement(IPrimitiveType<Date> approvalDate);

    String getStatus();

    void setStatus(String status);

    ICompositeType getEffectivePeriod();

    boolean getExperimental();

    void setExtension(List<IBaseExtension<?, ?>> extensions);

    @SuppressWarnings("unchecked")
    static <T extends ICompositeType & IBaseHasExtensions> T newRelatedArtifact(
            FhirVersionEnum version, String type, String reference) {
        switch (version) {
            case DSTU3:
                var dstu3 = new org.hl7.fhir.dstu3.model.RelatedArtifact();
                dstu3.setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.fromCode(type))
                        .setResource(new Reference(reference));
                return (T) dstu3;
            case R4:
                var r4 = new org.hl7.fhir.r4.model.RelatedArtifact();
                r4.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.fromCode(type))
                        .setResource(reference);
                return (T) r4;
            case R5:
                var r5 = new org.hl7.fhir.r5.model.RelatedArtifact();
                r5.setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.fromCode(type))
                        .setResource(reference);
                return (T) r5;

            default:
                throw new UnprocessableEntityException("Upsupported version: " + version.toString());
        }
    }

    static <T extends ICompositeType & IBaseHasExtensions> String getRelatedArtifactReference(T relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
            return getRelatedArtifactReference((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact);
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
            return getRelatedArtifactReference((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact);
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
            return getRelatedArtifactReference((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact);
        } else {
            throw new UnprocessableEntityException("Must be a valid RelatedArtifact");
        }
    }

    private static String getRelatedArtifactReference(org.hl7.fhir.dstu3.model.RelatedArtifact relatedArtifact) {
        return relatedArtifact.getResource().getReference();
    }

    private static String getRelatedArtifactReference(org.hl7.fhir.r4.model.RelatedArtifact relatedArtifact) {
        return relatedArtifact.getResource();
    }

    private static String getRelatedArtifactReference(org.hl7.fhir.r5.model.RelatedArtifact relatedArtifact) {
        return relatedArtifact.getResource();
    }

    static <T extends ICompositeType & IBaseHasExtensions> String getRelatedArtifactType(T relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
            return getRelatedArtifactType((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact);
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
            return getRelatedArtifactType((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact);
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
            return getRelatedArtifactType((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact);
        } else {
            throw new UnprocessableEntityException("Must be a valid RelatedArtifact");
        }
    }

    private static String getRelatedArtifactType(org.hl7.fhir.dstu3.model.RelatedArtifact relatedArtifact) {
        return relatedArtifact.getType().toCode();
    }

    private static String getRelatedArtifactType(org.hl7.fhir.r4.model.RelatedArtifact relatedArtifact) {
        return relatedArtifact.getType().toCode();
    }

    private static String getRelatedArtifactType(org.hl7.fhir.r5.model.RelatedArtifact relatedArtifact) {
        return relatedArtifact.getType().toCode();
    }

    static <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifactReference(
            T relatedArtifact, String reference) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
            setRelatedArtifactReference((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact, reference);
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
            setRelatedArtifactReference((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact, reference);
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
            setRelatedArtifactReference((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact, reference);
        } else {
            throw new UnprocessableEntityException("Must be a valid RelatedArtifact");
        }
    }
    ;

    private static void setRelatedArtifactReference(
            org.hl7.fhir.dstu3.model.RelatedArtifact relatedArtifact, String reference) {
        relatedArtifact.getResource().setReference(reference);
    }

    private static void setRelatedArtifactReference(
            org.hl7.fhir.r4.model.RelatedArtifact relatedArtifact, String reference) {
        relatedArtifact.setResource(reference);
    }

    private static void setRelatedArtifactReference(
            org.hl7.fhir.r5.model.RelatedArtifact relatedArtifact, String reference) {
        relatedArtifact.setResource(reference);
    }

    void setEffectivePeriod(ICompositeType effectivePeriod);

    boolean hasRelatedArtifact();

    <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifact();

    <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifactsOfType(String codeString);

    default <T extends ICompositeType & IBaseHasExtensions> List<T> getComponents() {
        return this.getRelatedArtifactsOfType("composed-of");
    }

    static <T extends ICompositeType & IBaseHasExtensions> boolean checkIfRelatedArtifactIsOwned(T relatedArtifact) {
        return relatedArtifact.getExtension().stream()
                .anyMatch(ext -> ext.getUrl().equals(isOwnedUrl));
    }

    default List<IDependencyInfo> combineComponentsAndDependencies() {
        final String referenceSource = this.hasVersion() ? getUrl() + "|" + getVersion() : getUrl();
        return Stream.concat(
                        getComponents().stream()
                                .filter(ra -> ra != null)
                                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource)),
                        getDependencies().stream())
                .collect(Collectors.toList());
    }

    <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts);

    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters);

    @SuppressWarnings("unchecked")
    default <T extends ICompositeType & IBaseHasExtensions> List<T> getOwnedRelatedArtifacts() {
        return (List<T>) getRelatedArtifactsOfType("composed-of").stream()
                .filter(ra -> checkIfRelatedArtifactIsOwned(ra))
                .collect(Collectors.toList());
    }

    static boolean isSupportedMetadataResource(IBaseResource resource) {
        return resource instanceof org.hl7.fhir.dstu3.model.MetadataResource
                || resource instanceof org.hl7.fhir.r4.model.MetadataResource
                || resource instanceof org.hl7.fhir.r5.model.MetadataResource;
    }

    static Optional<IDomainResource> findLatestVersion(IBaseBundle bundle) {
        var sorted = BundleHelper.getEntryResources(bundle).stream()
                .filter(r -> isSupportedMetadataResource(r))
                .map(r -> (KnowledgeArtifactAdapter) AdapterFactory.forFhirVersion(r.getStructureFhirVersionEnum())
                        .createResource(r))
                .sorted((a, b) -> a.getVersion().compareTo(b.getVersion()))
                .collect(Collectors.toList());
        if (!sorted.isEmpty()) {
            return Optional.of(sorted.get(0).get());
        } else {
            return Optional.ofNullable(null);
        }
    }

    String releaseLabelUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
    String releaseDescriptionUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
    String usPhContextTypeUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
    String contextTypeUrl = "http://terminology.hl7.org/CodeSystem/usage-context-type";
    String contextUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";
    String isOwnedUrl = "http://hl7.org/fhir/StructureDefinition/crmi-isOwned";
}
