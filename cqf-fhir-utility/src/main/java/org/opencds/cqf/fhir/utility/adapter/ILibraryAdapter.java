package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.utility.Constants;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface ILibraryAdapter extends IKnowledgeArtifactAdapter {

    boolean hasContent();

    <T extends ICompositeType> List<T> getContent();

    void setContent(List<? extends ICompositeType> attachments);

    ICompositeType addContent();

    ICompositeType getType();

    ILibraryAdapter setType(String type);

    <T extends ICompositeType> List<T> getParameter();

    boolean hasDataRequirement();

    List<IDataRequirementAdapter> getDataRequirement();

    ILibraryAdapter addDataRequirement(ICompositeType dataRequirement);

    <T extends ICompositeType> ILibraryAdapter setDataRequirement(List<T> dataRequirement);

    void setExpansionParameters(IBaseParameters expansionParameters);

    void ensureExpansionParametersEntry(IKnowledgeArtifactAdapter adapter, String crmiVersion);

    default String getExpansionParameterName(String resourceType, String crmiVersion) {
        if (StringUtils.isBlank(resourceType)) {
            throw new IllegalArgumentException("Missing required parameter: 'resourceType'");
        }

        boolean isCrmiV1 = crmiVersion != null && crmiVersion.equals(Constants.CRMI_VERSION_1);

        if (resourceType.equals("CodeSystem")) {
            return isCrmiV1 ? Constants.SYSTEM_VERSION : Constants.DEFAULT_SYSTEM_VERSION;
        } else if (resourceType.equals("ValueSet")) {
            return isCrmiV1 ? Constants.CANONICAL_VERSION : Constants.DEFAULT_VALUESET_VERSION;
        } else {
            return isCrmiV1 ? Constants.CANONICAL_VERSION : Constants.DEFAULT_CANONICAL_VERSION;
        }
    }

    // For CRMI Version 1, entries for all resource types other than CodeSystem should have the
    // resourceType extension. For CRMI Version 2, only entries for ValueSets should have the
    // resourceType extension.
    default boolean shouldAddResourceTypeExtension(String crmiVersion, String resourceType) {
        return ((crmiVersion == null || crmiVersion.equals(Constants.CRMI_VERSION_1))
                        && !resourceType.equals(Constants.RESOURCETYPE_CODESYSTEM))
                || ((crmiVersion != null && !crmiVersion.equals(Constants.CRMI_VERSION_1))
                        && !resourceType.equals(Constants.RESOURCETYPE_VALUESET)
                        && !resourceType.equals(Constants.RESOURCETYPE_CODESYSTEM));
    }
}
