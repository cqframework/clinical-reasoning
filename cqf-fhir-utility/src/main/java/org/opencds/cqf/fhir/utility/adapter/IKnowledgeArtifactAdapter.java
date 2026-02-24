package org.opencds.cqf.fhir.utility.adapter;

import static java.util.stream.Collectors.toMap;
import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newDateTimeType;
import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newDateType;
import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newPeriod;
import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newStringType;
import static org.opencds.cqf.fhir.utility.adapter.IAdapter.newUriType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.VersionComparator;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IKnowledgeArtifactAdapter extends IResourceAdapter {
    public static final Logger logger = LoggerFactory.getLogger(IKnowledgeArtifactAdapter.class);
    static final String DEPENDSON = "depends-on";

    IDomainResource get();

    IDomainResource copy();

    default IIdType getId() {
        return get().getIdElement();
    }

    default void setId(IIdType id) {
        get().setId(id);
    }

    default boolean hasName() {
        return StringUtils.isNotBlank(getName());
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
        getModelResolver().setValue(get(), "title", newStringType(get().getStructureFhirVersionEnum(), title));
    }

    default String getDescriptor() {
        return "%s %s%s"
                .formatted(
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

    /**
     * Returns the url of the artifact appended with '|' version if the artifact has a version.
     * @return canonical url of artifact
     */
    default String getCanonical() {
        if (!hasUrl()) {
            return getId().getValueAsString();
        }
        return getUrl().concat(hasVersion() ? "|%s".formatted(getVersion()) : "");
    }

    List<IDependencyInfo> getDependencies();

    default List<IDependencyInfo> getDependencies(IRepository repository) {
        // TODO: this should be smarter
        return getDependencies();
    }

    default String getReferenceSource() {
        return hasVersion() ? getUrl() + "|" + getVersion() : getUrl();
    }

    default void addProfileReferences(List<IDependencyInfo> references, String referenceSource) {
        get().getMeta().getProfile().forEach(x -> {
            var p = (IPrimitiveType<String>) x;
            var e = (IBaseHasExtensions) x;
            references.add(new DependencyInfo(referenceSource, p.getValueAsString(), e.getExtension(), p::setValue));
        });
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

    <T extends ICompositeType> List<T> getUseContext();

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
        var experimental = resolvePath(get(), "experimental", IPrimitiveType.class);
        return experimental != null && ((IPrimitiveType<Boolean>) experimental).getValue();
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
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact artifact2) {
            return artifact2.getResource().getReference();
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact artifact1) {
            return artifact1.getResource();
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact artifact) {
            return artifact.getResource();
        } else {
            throw new UnprocessableEntityException(VALID_RELATED_ARTIFACT);
        }
    }

    static <T extends ICompositeType & IBaseHasExtensions> String getRelatedArtifactDisplay(T relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact artifact2) {
            return artifact2.getDisplay();
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact artifact1) {
            return artifact1.getDisplay();
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact artifact) {
            return artifact.getDisplay();
        } else {
            throw new UnprocessableEntityException(VALID_RELATED_ARTIFACT);
        }
    }

    static <T extends ICompositeType & IBaseHasExtensions> String getRelatedArtifactType(T relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact artifact2) {
            return artifact2.getType().toCode();
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact artifact1) {
            return artifact1.getType().toCode();
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact artifact) {
            return artifact.getType().toCode();
        } else {
            throw new UnprocessableEntityException(VALID_RELATED_ARTIFACT);
        }
    }

    static <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifactReference(
            T relatedArtifact, String reference, String display) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact artifact2) {
            artifact2.getResource().setReference(reference).setDisplay(display);
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact artifact1) {
            artifact1.setResource(reference).setDisplay(display);
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact artifact) {
            artifact.setResource(reference).setDisplay(display);
        } else {
            throw new UnprocessableEntityException(VALID_RELATED_ARTIFACT);
        }
    }

    default boolean hasRelatedArtifact() {
        return !getRelatedArtifact().isEmpty();
    }

    default <T extends ICompositeType & IBaseHasExtensions> void addRelatedArtifact(T relatedArtifact) {
        try {
            getModelResolver().setValue(get(), "relatedArtifact", List.of(relatedArtifact));
        } catch (Exception e) {
            // Do nothing
            logger.debug("Field 'relatedArtifact' does not exist on Resource type {}", get().fhirType());
        }
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
                .anyMatch(ext -> ext.getUrl().equals(IS_OWNED_URL));
    }

    @SuppressWarnings({"squid:S1612"})
    default List<IDependencyInfo> combineComponentsAndDependencies() {
        final String referenceSource = hasVersion() ? getUrl() + "|" + getVersion() : getUrl();
        return Stream.concat(
                        getComponents().stream()
                                .filter(Objects::nonNull)
                                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource)),
                        getDependencies().stream())
                .collect(Collectors.toList());
    }

    default IBase accept(IKnowledgeArtifactVisitor visitor, IBaseParameters operationParameters) {
        return visitor.visit(this, operationParameters);
    }

    @SuppressWarnings("unchecked")
    default <T extends ICompositeType & IBaseHasExtensions> List<T> getOwnedRelatedArtifacts() {
        return (List<T>) getRelatedArtifactsOfType("composed-of").stream()
                .filter(IKnowledgeArtifactAdapter::checkIfRelatedArtifactIsOwned)
                .collect(Collectors.toList());
    }

    static boolean isSupportedMetadataResource(IBaseResource resource) {
        return resource instanceof org.hl7.fhir.dstu3.model.MetadataResource
                || resource instanceof org.hl7.fhir.r4.model.MetadataResource
                || resource instanceof org.hl7.fhir.r5.model.MetadataResource;
    }

    static Optional<IDomainResource> findLatestVersion(IBaseBundle bundle) {
        var versionComparator = new VersionComparator();
        var sorted = BundleHelper.getEntryResources(bundle).stream()
                .filter(IKnowledgeArtifactAdapter::isSupportedMetadataResource)
                .map(r -> (IKnowledgeArtifactAdapter) IAdapterFactory.forFhirVersion(r.getStructureFhirVersionEnum())
                        .createResource(r))
                .sorted((a, b) -> versionComparator.compare(a.getVersion(), b.getVersion()))
                .toList();
        if (!sorted.isEmpty()) {
            return Optional.of(sorted.get(sorted.size() - 1).get());
        } else {
            return Optional.empty();
        }
    }

    default Optional<IBaseParameters> getExpansionParameters() {
        return Optional.empty();
    }

    default Map<String, String> getReferencedLibraries() {
        return resolveCqfLibraries();
    }

    default Map<String, ILibraryAdapter> retrieveReferencedLibraries(IRepository repository) {
        return getReferencedLibraries().values().stream()
                .map(url -> getAdapterFactory()
                        .createLibrary(SearchHelper.searchRepositoryByCanonical(
                                repository,
                                VersionUtilities.canonicalTypeForVersion(
                                        repository.fhirContext().getVersion().getVersion(), url))))
                .collect(toMap(IKnowledgeArtifactAdapter::getName, l -> l));
    }

    default Map<String, String> resolveCqfLibraries() {
        return getExtension().stream()
                .filter(e -> Constants.CQF_LIBRARY.equals(e.getUrl()))
                .map(e -> e.getValue())
                .filter(IPrimitiveType.class::isInstance)
                .map(IPrimitiveType.class::cast)
                .map(IPrimitiveType::getValueAsString)
                .map(l -> Map.entry(Canonicals.getIdPart(l), l))
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    String VALID_RELATED_ARTIFACT = "Must be a valid RelatedArtifact";
    String RELEASE_LABEL_URL = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
    String RELEASE_DESCRIPTION_URL = "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
    String US_PH_CONTEXT_TYPE_URL = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
    String CONTEXT_TYPE_URL = "http://terminology.hl7.org/CodeSystem/usage-context-type";
    String CONTEXT_URL = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";
    String IS_OWNED_URL = "http://hl7.org/fhir/StructureDefinition/artifact-isOwned";
}
