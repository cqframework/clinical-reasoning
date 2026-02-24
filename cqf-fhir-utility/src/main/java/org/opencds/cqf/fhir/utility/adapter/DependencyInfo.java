package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.utility.Constants;

public class DependencyInfo implements IDependencyInfo {
    // TODO: Need for figuring out how to determine which package the dependency is in.
    private String referenceSource;
    private String reference;
    private String referencePackageId;
    private Consumer<String> updateReferenceConsumer;
    private List<? extends IBaseExtension<?, ?>> extensionList;
    private List<String> roles = new ArrayList<>();
    private List<String> fhirPaths = new ArrayList<>();

    public DependencyInfo() {}

    public DependencyInfo(
            String referenceSource,
            String reference,
            List<? extends IBaseExtension<?, ?>> extensionList,
            Consumer<String> updateReferenceConsumer) {
        this.referenceSource = referenceSource;
        this.reference = reference;
        this.extensionList = extensionList;
        this.updateReferenceConsumer = updateReferenceConsumer;
    }

    public String getReferenceSource() {
        return this.referenceSource;
    }

    @SuppressWarnings("unchecked")
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
        this.updateReferenceConsumer.accept(reference);
    }

    public String getReferencePackageId() {
        return this.referencePackageId;
    }

    public void setReferencePackageId(String referencePackageId) {
        this.referencePackageId = referencePackageId;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void addRole(String role) {
        if (role != null && !this.roles.contains(role)) {
            this.roles.add(role);
        }
    }

    public List<String> getFhirPaths() {
        return this.fhirPaths;
    }

    public void addFhirPath(String fhirPath) {
        if (fhirPath != null && !this.fhirPaths.contains(fhirPath)) {
            this.fhirPaths.add(fhirPath);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends IBaseExtension<?, ?>> List<E> buildDependencyExtensions(
            FhirVersionEnum fhirVersion, String sourceArtifactUrl) {
        List<E> extensions = new ArrayList<>();

        // Add role extensions
        for (String role : roles) {
            E roleExt = buildDependencyRoleExtension(fhirVersion, role);
            if (roleExt != null) {
                extensions.add(roleExt);
            }
        }

        // Add package-source extension if available
        if (referencePackageId != null && !referencePackageId.isEmpty()) {
            E packageExt = buildPackageSourceExtension(fhirVersion, referencePackageId);
            if (packageExt != null) {
                extensions.add(packageExt);
            }
        }

        // Add reference-source extensions for each FHIRPath
        if (sourceArtifactUrl != null && !sourceArtifactUrl.isEmpty()) {
            for (String fhirPath : fhirPaths) {
                E refSourceExt = buildReferenceSourceExtension(fhirVersion, sourceArtifactUrl, fhirPath);
                if (refSourceExt != null) {
                    extensions.add(refSourceExt);
                }
            }
        }

        return extensions;
    }

    @SuppressWarnings("unchecked")
    private <E extends IBaseExtension<?, ?>> E buildDependencyRoleExtension(
            FhirVersionEnum fhirVersion, String roleCode) {
        return switch (fhirVersion) {
            case DSTU3 ->
                (E) new org.hl7.fhir.dstu3.model.Extension(
                        Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.dstu3.model.CodeType(roleCode));
            case R4 ->
                (E) new org.hl7.fhir.r4.model.Extension(
                        Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r4.model.CodeType(roleCode));
            case R5 ->
                (E) new org.hl7.fhir.r5.model.Extension(
                        Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r5.model.CodeType(roleCode));
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private <E extends IBaseExtension<?, ?>> E buildPackageSourceExtension(
            FhirVersionEnum fhirVersion, String packageSource) {
        return switch (fhirVersion) {
            case DSTU3 ->
                (E) new org.hl7.fhir.dstu3.model.Extension(
                        Constants.PACKAGE_SOURCE, new org.hl7.fhir.dstu3.model.StringType(packageSource));
            case R4 ->
                (E) new org.hl7.fhir.r4.model.Extension(
                        Constants.PACKAGE_SOURCE, new org.hl7.fhir.r4.model.StringType(packageSource));
            case R5 ->
                (E) new org.hl7.fhir.r5.model.Extension(
                        Constants.PACKAGE_SOURCE, new org.hl7.fhir.r5.model.StringType(packageSource));
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private <E extends IBaseExtension<?, ?>> E buildReferenceSourceExtension(
            FhirVersionEnum fhirVersion, String artifactCanonical, String fhirPath) {
        return switch (fhirVersion) {
            case DSTU3 -> {
                var ext = new org.hl7.fhir.dstu3.model.Extension(Constants.CRMI_REFERENCE_SOURCE);
                ext.addExtension("artifact", new org.hl7.fhir.dstu3.model.UriType(artifactCanonical));
                ext.addExtension("path", new org.hl7.fhir.dstu3.model.StringType(fhirPath));
                yield (E) ext;
            }
            case R4 -> {
                var ext = new org.hl7.fhir.r4.model.Extension(Constants.CRMI_REFERENCE_SOURCE);
                ext.addExtension("artifact", new org.hl7.fhir.r4.model.CanonicalType(artifactCanonical));
                ext.addExtension("path", new org.hl7.fhir.r4.model.StringType(fhirPath));
                yield (E) ext;
            }
            case R5 -> {
                var ext = new org.hl7.fhir.r5.model.Extension(Constants.CRMI_REFERENCE_SOURCE);
                ext.addExtension("artifact", new org.hl7.fhir.r5.model.CanonicalType(artifactCanonical));
                ext.addExtension("path", new org.hl7.fhir.r5.model.StringType(fhirPath));
                yield (E) ext;
            }
            default -> null;
        };
    }

    public static IDependencyInfo convertRelatedArtifact(ICompositeType ra, String source) {
        if (ra instanceof org.hl7.fhir.dstu3.model.RelatedArtifact reference) {
            return new DependencyInfo(
                    source, reference.getResource().getReference(), reference.getExtension(), ref -> reference
                            .getResource()
                            .setReference(ref));
        } else if (ra instanceof org.hl7.fhir.r4.model.RelatedArtifact reference) {
            return new DependencyInfo(
                    source, reference.getResource(), reference.getExtension(), reference::setResource);

        } else if (ra instanceof org.hl7.fhir.r5.model.RelatedArtifact reference) {
            // R5 can have either a Resource (canonical URL) or a ResourceReference
            //      we'll take the canonicalURL if it's there, but fallback to ResourceReference
            //      if it's not.
            return new DependencyInfo(
                    source,
                    ObjectUtils.firstNonNull(
                            reference.getResource(),
                            reference.getResourceReference() == null
                                    ? null
                                    : reference.getResourceReference().getReference()),
                    reference.getExtension(),
                    reference::setResource);
        } else {
            throw new UnprocessableEntityException("A valid RelatedArtifact object must be provided");
        }
    }
}
