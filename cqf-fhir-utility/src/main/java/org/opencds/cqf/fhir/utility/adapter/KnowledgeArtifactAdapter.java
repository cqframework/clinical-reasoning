package org.opencds.cqf.fhir.utility.adapter;

import static org.opencds.cqf.fhir.utility.adapter.Adapter.newDateTimeType;
import static org.opencds.cqf.fhir.utility.adapter.Adapter.newDateType;
import static org.opencds.cqf.fhir.utility.adapter.Adapter.newPeriod;
import static org.opencds.cqf.fhir.utility.adapter.Adapter.newStringType;
import static org.opencds.cqf.fhir.utility.adapter.Adapter.newUriType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Versions;
import org.opencds.cqf.fhir.utility.visitor.IKnowledgeArtifactVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface KnowledgeArtifactAdapter extends ResourceAdapter {
    public static final Logger logger = LoggerFactory.getLogger(KnowledgeArtifactAdapter.class);

    IDomainResource get();

    IDomainResource copy();

    default IIdType getId() {
        return get().getIdElement();
    }

    default void setId(IIdType id) {
        get().setId(id);
    }

    default String getName() {
        return resolvePathString(get(), "name");
    }

    default void setName(String name) {
        getModelResolver().setValue(get(), "name", newStringType(get().getStructureFhirVersionEnum(), name));
    }

    default boolean hasTitle() {
        return StringUtils.isNotBlank(getTitle());
    }

    default String getTitle() {
        return resolvePathString(get(), "title");
    }

    default void setTitle(String title) {
        getModelResolver().setValue(get(), "title", title);
    }

    default String getDescriptor() {
        return String.format(
                "%s %s%s",
                this.get().fhirType(),
                this.hasTitle() ? this.getTitle() : this.getName(),
                this.hasVersion() ? ", " + this.getVersion() : "");
    }

    default boolean hasUrl() {
        return StringUtils.isNotBlank(getUrl());
    }

    default String getUrl() {
        return resolvePathString(get(), "url");
    }

    default void setUrl(String url) {
        getModelResolver().setValue(get(), "url", newUriType(get().getStructureFhirVersionEnum(), url));
    }

    default boolean hasVersion() {
        return StringUtils.isNotBlank(getVersion());
    }

    default String getVersion() {
        return resolvePathString(get(), "version");
    }

    default void setVersion(String version) {
        getModelResolver().setValue(get(), "version", newStringType(get().getStructureFhirVersionEnum(), version));
    }

    List<IDependencyInfo> getDependencies();

    default String getReferenceSource() {
        return hasVersion() ? getUrl() + "|" + getVersion() : getUrl();
    }

    default void addProfileReferences(List<IDependencyInfo> references, String referenceSource) {
        get().getMeta().getProfile().stream()
                .map(p -> (IBaseHasExtensions & IPrimitiveType<String>) p)
                .forEach(profile -> references.add(new DependencyInfo(
                        referenceSource,
                        profile.getValueAsString(),
                        profile.getExtension(),
                        (reference) -> profile.setValue(reference))));
    }

    @SuppressWarnings("unchecked")
    default Date getApprovalDate() {
        IPrimitiveType<Date> approvalDate = resolvePath(get(), "approvalDate", IPrimitiveType.class);
        return approvalDate == null ? null : approvalDate.getValue();
    }

    default void setApprovalDate(Date approvalDate) {
        try {
            getModelResolver()
                    .setValue(get(), "approvalDate", newDateType(get().getStructureFhirVersionEnum(), approvalDate));
        } catch (Exception e) {
            // Do nothing
            logger.debug("Field 'approvalDate' does not exist on Resource type {}", get().fhirType());
        }
    }

    @SuppressWarnings("unchecked")
    default Date getDate() {
        IPrimitiveType<Date> date = resolvePath(get(), "date", IPrimitiveType.class);
        return date == null ? null : date.getValue();
    }

    default void setDate(Date date) {
        getModelResolver().setValue(get(), "date", newDateTimeType(get().getStructureFhirVersionEnum(), date));
    }

    default void setDateElement(IPrimitiveType<Date> date) {
        getModelResolver().setValue(get(), "date", date);
    }

    default String getPurpose() {
        return resolvePathString(get(), "purpose");
    }

    String getStatus();

    void setStatus(String status);

    default ICompositeType getEffectivePeriod() {
        var effectivePeriod = resolvePath(get(), "effectivePeriod", ICompositeType.class);
        return effectivePeriod == null ? newPeriod(get().getStructureFhirVersionEnum()) : effectivePeriod;
    }

    default void setEffectivePeriod(ICompositeType period) {
        try {
            getModelResolver().setValue(get(), "effectivePeriod", period);
        } catch (Exception e) {
            // Do nothing
            logger.debug("Field 'effectivePeriod' does not exist on Resource type {}", get().fhirType());
        }
    }

    @SuppressWarnings("unchecked")
    default boolean getExperimental() {
        var experimental = (IPrimitiveType<Boolean>) resolvePath(get(), "experimental", IPrimitiveType.class);
        return experimental == null ? false : experimental.getValue();
    }

    @SuppressWarnings("unchecked")
    static <T extends ICompositeType & IBaseHasExtensions> T newRelatedArtifact(
            FhirVersionEnum version, String type, String reference, String display) {
        switch (version) {
            case DSTU3:
                var dstu3 = new org.hl7.fhir.dstu3.model.RelatedArtifact();
                dstu3.setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.fromCode(type))
                        .setResource(new Reference(reference))
                        .setDisplay(display);
                return (T) dstu3;
            case R4:
                var r4 = new org.hl7.fhir.r4.model.RelatedArtifact();
                r4.setType(org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.fromCode(type))
                        .setResource(reference)
                        .setDisplay(display);
                return (T) r4;
            case R5:
                var r5 = new org.hl7.fhir.r5.model.RelatedArtifact();
                r5.setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.fromCode(type))
                        .setResource(reference)
                        .setDisplay(display);
                return (T) r5;

            default:
                throw new UnprocessableEntityException("Unsupported version: " + version.toString());
        }
    }

    static <T extends ICompositeType & IBaseHasExtensions> String getRelatedArtifactReference(T relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
            return ((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact)
                    .getResource()
                    .getReference();
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
            return ((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact).getResource();
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
            return ((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact).getResource();
        } else {
            throw new UnprocessableEntityException("Must be a valid RelatedArtifact");
        }
    }

    static <T extends ICompositeType & IBaseHasExtensions> String getRelatedArtifactType(T relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
            return ((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact)
                    .getType()
                    .toCode();
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
            return ((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact)
                    .getType()
                    .toCode();
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
            return ((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact)
                    .getType()
                    .toCode();
        } else {
            throw new UnprocessableEntityException("Must be a valid RelatedArtifact");
        }
    }

    static <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifactReference(
            T relatedArtifact, String reference, String display) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact) {
            ((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact)
                    .getResource()
                    .setReference(reference)
                    .setDisplay(display);
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact) {
            ((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact)
                    .setResource(reference)
                    .setDisplay(display);
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact) {
            ((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact)
                    .setResource(reference)
                    .setDisplay(display);
        } else {
            throw new UnprocessableEntityException("Must be a valid RelatedArtifact");
        }
    }

    default boolean hasRelatedArtifact() {
        return !getRelatedArtifact().isEmpty();
    }

    @SuppressWarnings("unchecked")
    default <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifact() {
        return resolvePathList(get(), "relatedArtifact").stream()
                .map(r -> (T) r)
                .collect(Collectors.toList());
    }

    default <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts) {
        try {
            getModelResolver().setValue(get(), "relatedArtifact", null);
            getModelResolver().setValue(get(), "relatedArtifact", relatedArtifacts);
        } catch (Exception e) {
            // Do nothing
            logger.debug("Field 'relatedArtifact' does not exist on Resource type {}", get().fhirType());
        }
    }

    <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifactsOfType(String codeString);

    default <T extends ICompositeType & IBaseHasExtensions> List<T> getComponents() {
        return getRelatedArtifactsOfType("composed-of");
    }

    static <T extends ICompositeType & IBaseHasExtensions> boolean checkIfRelatedArtifactIsOwned(T relatedArtifact) {
        return relatedArtifact.getExtension().stream()
                .anyMatch(ext -> ext.getUrl().equals(isOwnedUrl));
    }

    default List<IDependencyInfo> combineComponentsAndDependencies() {
        final String referenceSource = hasVersion() ? getUrl() + "|" + getVersion() : getUrl();
        return Stream.concat(
                        getComponents().stream()
                                .filter(ra -> ra != null)
                                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource)),
                        getDependencies().stream())
                .collect(Collectors.toList());
    }

    default IBase accept(
            IKnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

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
                .sorted((a, b) -> Versions.compareVersions(a.getVersion(), b.getVersion()))
                .collect(Collectors.toList());
        if (!sorted.isEmpty()) {
            return Optional.of(sorted.get(0).get());
        } else {
            return Optional.ofNullable(null);
        }
    }

    default Optional<IBaseParameters> getExpansionParameters() {
        return Optional.empty();
    }

    String releaseLabelUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
    String releaseDescriptionUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
    String usPhContextTypeUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
    String contextTypeUrl = "http://terminology.hl7.org/CodeSystem/usage-context-type";
    String contextUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";
    String isOwnedUrl = "http://hl7.org/fhir/StructureDefinition/crmi-isOwned";
}
