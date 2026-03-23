package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor that implements the $infer-manifest-parameters operation.
 * <p>
 * Takes a module-definition Library (output of $data-requirements operation) and converts
 * its relatedArtifacts into manifest expansion parameters following CRMI conventions:
 * <ul>
 *   <li>CodeSystem → system-version parameter (format: "system|version")</li>
 *   <li>ValueSet → canonicalVersion parameter</li>
 *   <li>Other resources → canonicalVersion parameter with resourceType extension</li>
 * </ul>
 */
public class InferManifestParametersVisitor extends BaseKnowledgeArtifactVisitor {
    private static final Logger logger = LoggerFactory.getLogger(InferManifestParametersVisitor.class);
    private static final String EXPANSION_PARAMETERS_REF = "#expansion-parameters";
    private static final String COMPOSED_OF = "composed-of";
    private static final String DEPENDS_ON = "depends-on";

    public InferManifestParametersVisitor(IRepository repository) {
        super(repository);
    }

    @Override
    public IBase visit(IKnowledgeArtifactAdapter adapter, IBaseParameters operationParameters) {
        // Validate input is a Library
        if (!"Library".equals(adapter.get().fhirType())) {
            throw new UnprocessableEntityException(
                    "Input resource must be a Library, found: " + adapter.get().fhirType());
        }

        var inputLibrary = (ILibraryAdapter) adapter;

        // Validate it's a module-definition Library
        var typeAdapter = inputLibrary.getType();
        if (typeAdapter != null) {
            String type = typeAdapter.fhirType().equals("CodeableConcept")
                    ? extractCodeableConceptCode(typeAdapter)
                    : typeAdapter.toString();
            if (type == null || !type.contains("module-definition")) {
                logger.warn("Input Library type is '{}', expected 'module-definition'. Processing anyway.", type);
            }
        }

        // Create manifest Library (asset-collection)
        var fhirVersion = fhirContext().getVersion().getVersion();

        return switch (fhirVersion) {
            case DSTU3 -> createDstu3Manifest(inputLibrary);
            case R4 -> createR4Manifest(inputLibrary);
            case R5 -> createR5Manifest(inputLibrary);
            default -> throw new UnprocessableEntityException("Unsupported FHIR version: " + fhirVersion);
        };
    }

    private String extractCodeableConceptCode(IBase codeableConcept) {
        if (codeableConcept instanceof org.hl7.fhir.dstu3.model.CodeableConcept dstu3CC) {
            if (dstu3CC.hasCoding() && !dstu3CC.getCoding().isEmpty()) {
                return dstu3CC.getCoding().get(0).getCode();
            }
        } else if (codeableConcept instanceof org.hl7.fhir.r4.model.CodeableConcept r4CC) {
            if (r4CC.hasCoding() && !r4CC.getCoding().isEmpty()) {
                return r4CC.getCoding().get(0).getCode();
            }
        } else if (codeableConcept instanceof org.hl7.fhir.r5.model.CodeableConcept r5CC) {
            if (r5CC.hasCoding() && !r5CC.getCoding().isEmpty()) {
                return r5CC.getCoding().get(0).getCode();
            }
        }
        return null;
    }

    private org.hl7.fhir.dstu3.model.Library createDstu3Manifest(ILibraryAdapter inputLibrary) {
        var manifest = new org.hl7.fhir.dstu3.model.Library();

        // Set metadata: manifest is a generated artifact
        manifest.setUrl(inputLibrary.getUrl());
        manifest.setVersion(inputLibrary.getVersion());
        manifest.setName(inputLibrary.getName() != null ? inputLibrary.getName() + "Manifest" : "Manifest");
        manifest.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.DRAFT);
        manifest.setExperimental(true);
        manifest.setDate(new Date());

        var typeCC = new org.hl7.fhir.dstu3.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("asset-collection");
        manifest.setType(typeCC);

        // Create Parameters resource for expansion parameters
        var parameters = new org.hl7.fhir.dstu3.model.Parameters();

        // Process relatedArtifacts
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            processRelatedArtifactDstu3(relatedArtifact, parameters, inputLibrary);
        }

        // Store parameters as a contained resource with cqf-expansionParameters extension
        if (!parameters.getParameter().isEmpty()) {
            parameters.setId("expansion-parameters");
            manifest.getContained().add(parameters);
            manifest.addExtension(
                    Constants.CQF_EXPANSION_PARAMETERS,
                    new org.hl7.fhir.dstu3.model.Reference(EXPANSION_PARAMETERS_REF));
        }

        // Copy composed-of and depends-on relatedArtifacts to manifest
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            String raType = extractRelatedArtifactType(relatedArtifact);
            if (COMPOSED_OF.equals(raType) || DEPENDS_ON.equals(raType)) {
                manifest.addRelatedArtifact((org.hl7.fhir.dstu3.model.RelatedArtifact) relatedArtifact);
            }
        }

        return manifest;
    }

    private org.hl7.fhir.r4.model.Library createR4Manifest(ILibraryAdapter inputLibrary) {
        var manifest = new org.hl7.fhir.r4.model.Library();

        // Set metadata: manifest is a generated artifact
        manifest.setUrl(inputLibrary.getUrl());
        manifest.setVersion(inputLibrary.getVersion());
        manifest.setName(inputLibrary.getName() != null ? inputLibrary.getName() + "Manifest" : "Manifest");
        manifest.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.DRAFT);
        manifest.setExperimental(true);
        manifest.setDate(new Date());

        var typeCC = new org.hl7.fhir.r4.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("asset-collection");
        manifest.setType(typeCC);

        // Create Parameters resource for expansion parameters
        var parameters = new org.hl7.fhir.r4.model.Parameters();

        // Process relatedArtifacts
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            processRelatedArtifactR4(relatedArtifact, parameters, inputLibrary);
        }

        // Store parameters as a contained resource with cqf-expansionParameters extension
        if (!parameters.getParameter().isEmpty()) {
            parameters.setId("expansion-parameters");
            manifest.getContained().add(parameters);
            manifest.addExtension(
                    Constants.CQF_EXPANSION_PARAMETERS, new org.hl7.fhir.r4.model.Reference(EXPANSION_PARAMETERS_REF));
        }

        // Copy composed-of and depends-on relatedArtifacts to manifest
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            String raType = extractRelatedArtifactType(relatedArtifact);
            if (COMPOSED_OF.equals(raType) || DEPENDS_ON.equals(raType)) {
                manifest.addRelatedArtifact((org.hl7.fhir.r4.model.RelatedArtifact) relatedArtifact);
            }
        }

        return manifest;
    }

    private org.hl7.fhir.r5.model.Library createR5Manifest(ILibraryAdapter inputLibrary) {
        var manifest = new org.hl7.fhir.r5.model.Library();

        // Set metadata: manifest is a generated artifact
        manifest.setUrl(inputLibrary.getUrl());
        manifest.setVersion(inputLibrary.getVersion());
        manifest.setName(inputLibrary.getName() != null ? inputLibrary.getName() + "Manifest" : "Manifest");
        manifest.setStatus(org.hl7.fhir.r5.model.Enumerations.PublicationStatus.DRAFT);
        manifest.setExperimental(true);
        manifest.setDate(new Date());

        var typeCC = new org.hl7.fhir.r5.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("asset-collection");
        manifest.setType(typeCC);

        // Create Parameters resource for expansion parameters
        var parameters = new org.hl7.fhir.r5.model.Parameters();

        // Process relatedArtifacts
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            processRelatedArtifactR5(relatedArtifact, parameters, inputLibrary);
        }

        // Store parameters as a contained resource with cqf-expansionParameters extension
        if (!parameters.getParameter().isEmpty()) {
            parameters.setId("expansion-parameters");
            manifest.getContained().add(parameters);
            manifest.addExtension(
                    Constants.CQF_EXPANSION_PARAMETERS, new org.hl7.fhir.r5.model.Reference(EXPANSION_PARAMETERS_REF));
        }

        // Copy composed-of and depends-on relatedArtifacts to manifest
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            String raType = extractRelatedArtifactType(relatedArtifact);
            if (COMPOSED_OF.equals(raType) || DEPENDS_ON.equals(raType)) {
                manifest.addRelatedArtifact((org.hl7.fhir.r5.model.RelatedArtifact) relatedArtifact);
            }
        }

        return manifest;
    }

    private void processRelatedArtifactDstu3(
            ICompositeType relatedArtifact, org.hl7.fhir.dstu3.model.Parameters parameters, ILibraryAdapter library) {
        String raType = extractRelatedArtifactType(relatedArtifact);
        if (!DEPENDS_ON.equals(raType)) {
            return;
        }
        if (!hasKeyDependencyRole(relatedArtifact)) {
            return;
        }

        var canonical = extractCanonical(relatedArtifact, library);
        if (canonical == null) {
            return;
        }

        String resourceType = extractResourceType(relatedArtifact, canonical);
        if (resourceType == null) {
            return;
        }

        switch (resourceType) {
            case "CodeSystem":
                parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.dstu3.model.StringType(canonical));
                break;
            case "ValueSet":
                parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.dstu3.model.StringType(canonical));
                break;
            default:
                break;
        }
    }

    private void processRelatedArtifactR4(
            ICompositeType relatedArtifact, org.hl7.fhir.r4.model.Parameters parameters, ILibraryAdapter library) {
        String raType = extractRelatedArtifactType(relatedArtifact);
        if (!DEPENDS_ON.equals(raType)) {
            return;
        }
        if (!hasKeyDependencyRole(relatedArtifact)) {
            return;
        }

        var canonical = extractCanonical(relatedArtifact, library);
        if (canonical == null) {
            return;
        }

        String resourceType = extractResourceType(relatedArtifact, canonical);
        if (resourceType == null) {
            return;
        }

        switch (resourceType) {
            case "CodeSystem":
                parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.r4.model.StringType(canonical));
                break;
            case "ValueSet":
                parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r4.model.StringType(canonical));
                break;
            default:
                break;
        }
    }

    private void processRelatedArtifactR5(
            ICompositeType relatedArtifact, org.hl7.fhir.r5.model.Parameters parameters, ILibraryAdapter library) {
        String raType = extractRelatedArtifactType(relatedArtifact);
        if (!DEPENDS_ON.equals(raType)) {
            return;
        }
        if (!hasKeyDependencyRole(relatedArtifact)) {
            return;
        }

        var canonical = extractCanonical(relatedArtifact, library);
        if (canonical == null) {
            return;
        }

        String resourceType = extractResourceType(relatedArtifact, canonical);
        if (resourceType == null) {
            return;
        }

        switch (resourceType) {
            case "CodeSystem":
                parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.r5.model.StringType(canonical));
                break;
            case "ValueSet":
                parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r5.model.StringType(canonical));
                break;
            default:
                break;
        }
    }

    private String extractResourceType(ICompositeType relatedArtifact, String canonical) {
        // Try cqf-resourceType extension on the resource element
        if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact r4Ra) {
            var ext = r4Ra.getResourceElement().getExtensionByUrl(Constants.CQF_RESOURCETYPE);
            if (ext != null && ext.getValue() instanceof IPrimitiveType<?> code) {
                return code.getValueAsString();
            }
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact r5Ra) {
            var ext = r5Ra.getResourceElement().getExtensionByUrl(Constants.CQF_RESOURCETYPE);
            if (ext != null && ext.getValue() instanceof IPrimitiveType<?> code) {
                return code.getValueAsString();
            }
        } else if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact dstu3Ra
                && dstu3Ra.hasResource()) {
            var ext = dstu3Ra.getResource().getExtensionByUrl(Constants.CQF_RESOURCETYPE);
            if (ext != null && ext.getValue() instanceof IPrimitiveType<?> code) {
                return code.getValueAsString();
            }
        }
        // Fallback to URL parsing
        return Canonicals.getResourceType(canonical);
    }

    private String extractCanonical(ICompositeType relatedArtifact, ILibraryAdapter library) {
        var resourcePath = library.resolvePath(relatedArtifact, "resource");

        if (resourcePath instanceof IPrimitiveType<?> primitive) {
            return primitive.getValueAsString();
        } else if (resourcePath instanceof org.hl7.fhir.dstu3.model.Reference dstu3Ref) {
            return dstu3Ref.getReference();
        }

        return null;
    }

    private String extractRelatedArtifactType(ICompositeType relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact dstu3Ra) {
            return dstu3Ra.getType() != null ? dstu3Ra.getType().toCode() : null;
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact r4Ra) {
            return r4Ra.getType() != null ? r4Ra.getType().toCode() : null;
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact r5Ra) {
            return r5Ra.getType() != null ? r5Ra.getType().toCode() : null;
        }
        return null;
    }

    private boolean hasKeyDependencyRole(ICompositeType relatedArtifact) {
        List<?> extensions = getExtensions(relatedArtifact);

        if (extensions == null || extensions.isEmpty()) {
            return true; // backward compat: no dependencyRole extensions → treat as eligible
        }

        boolean hasDependencyRoleExt = false;
        boolean hasKeyRole = false;

        for (Object ext : extensions) {
            if (ext instanceof IBaseExtension<?, ?> baseExt
                    && Constants.CRMI_DEPENDENCY_ROLE.equals(baseExt.getUrl())) {
                hasDependencyRoleExt = true;
                if (baseExt.getValue() instanceof IPrimitiveType<?> primitive
                        && "key".equals(primitive.getValueAsString())) {
                    hasKeyRole = true;
                }
            }
        }

        // If no dependencyRole extensions at all, backward compat: treat as eligible
        return !hasDependencyRoleExt || hasKeyRole;
    }

    private List<?> getExtensions(ICompositeType relatedArtifact) {
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact dstu3Ra) {
            return dstu3Ra.getExtension();
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact r4Ra) {
            return r4Ra.getExtension();
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact r5Ra) {
            return r5Ra.getExtension();
        }
        return Collections.emptyList();
    }
}
