package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
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

    default void ensureExpansionParametersEntry(IKnowledgeArtifactAdapter artifactAdapter, String crmiVersion) {
        var maybeExpansionParameters = this.getExpansionParameters();
        if (maybeExpansionParameters.isPresent()) {
            IBaseParameters expansionParameters = maybeExpansionParameters.get();
            var resourceType = artifactAdapter.get().fhirType();
            var url = artifactAdapter.getUrl();
            var canonical = url + "|" + artifactAdapter.getVersion();
            var parameterName = this.getExpansionParameterName(resourceType, crmiVersion);
            if (!this.parameterExists(expansionParameters, parameterName, canonical)) {
                IPrimitiveType<String> canonicalToAdd =
                        buildCanonicalToAdd(artifactAdapter, crmiVersion, resourceType, canonical, this.fhirContext());
                ParametersUtil.addParameterToParameters(
                        this.fhirContext(), expansionParameters, parameterName, canonicalToAdd);
            }
        }
    }

    default boolean parameterExists(IBaseParameters parameters, String parameterName, String canonical) {
        if (parameters == null) {
            return false;
        } else {
            List<String> nameMatchedParameters =
                    ParametersUtil.getNamedParameterValuesAsString(this.fhirContext(), parameters, parameterName);
            return nameMatchedParameters.stream().anyMatch(p -> p.equals(canonical));
        }
    }

    /**
     * Build a canonical-like primitive with extensions in a FHIR-version-agnostic way.
     *
     * @param artifactAdapter Adapter providing descriptor
     * @param crmiVersion     CRMI version
     * @param resourceType    Resource type string
     * @param canonical       Canonical value
     * @param ctx             FhirContext for the version in use
     * @return IPrimitiveType<String> with extensions added
     */
    private IPrimitiveType<String> buildCanonicalToAdd(
            IKnowledgeArtifactAdapter artifactAdapter,
            String crmiVersion,
            String resourceType,
            String canonical,
            FhirContext ctx) {

        @SuppressWarnings("unchecked")
        IPrimitiveType<String> canonicalToAdd =
                (IPrimitiveType<String>) ctx.getElementDefinition("canonical").newInstance();
        canonicalToAdd.setValueAsString(canonical);

        if (canonicalToAdd instanceof IBaseHasExtensions hasExtensions) {

            // Helper to add to extension list
            @SuppressWarnings("unchecked")
            List<IBaseExtension<?, ?>> extensions = (List<IBaseExtension<?, ?>>) hasExtensions.getExtension();

            // ResourceType extension
            if (shouldAddResourceTypeExtension(crmiVersion, resourceType)) {
                IBaseExtension<?, ?> resourceTypeExt = (IBaseExtension<?, ?>)
                        ctx.getElementDefinition("Extension").newInstance();
                resourceTypeExt.setUrl(Constants.CQF_RESOURCETYPE);

                @SuppressWarnings("unchecked")
                IPrimitiveType<String> codeValue = (IPrimitiveType<String>)
                        ctx.getElementDefinition("code").newInstance();
                codeValue.setValueAsString(resourceType);

                resourceTypeExt.setValue(codeValue);
                extensions.add(resourceTypeExt);
            }

            // Display extension
            IBaseExtension<?, ?> displayExt =
                    (IBaseExtension<?, ?>) ctx.getElementDefinition("Extension").newInstance();
            displayExt.setUrl(Constants.DISPLAY_EXTENSION);

            @SuppressWarnings("unchecked")
            IPrimitiveType<String> displayValue =
                    (IPrimitiveType<String>) ctx.getElementDefinition("string").newInstance();
            displayValue.setValueAsString(artifactAdapter.getDescriptor());

            displayExt.setValue(displayValue);
            extensions.add(displayExt);
        }

        return canonicalToAdd;
    }

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
        var isV1AndQualifies = (crmiVersion == null || crmiVersion.equals(Constants.CRMI_VERSION_1))
                && !resourceType.equals(Constants.RESOURCETYPE_CODESYSTEM);

        var isV2AndQualifies = (crmiVersion != null && !crmiVersion.equals(Constants.CRMI_VERSION_1))
                && resourceType.equals(Constants.RESOURCETYPE_VALUESET);

        return isV1AndQualifies || isV2AndQualifies;
    }

    default void addCqfMessagesExtension(IBaseOperationOutcome messages) {
        getContained().add(messages);
        var ext = addExtension();
        ext.setUrl("http://hl7.org/fhir/StructureDefinition/cqf-messages");
        var ref =
                (IBaseReference) fhirContext().getElementDefinition("Reference").newInstance();
        ext.setValue(ref.setReference("#messages"));
    }
}
