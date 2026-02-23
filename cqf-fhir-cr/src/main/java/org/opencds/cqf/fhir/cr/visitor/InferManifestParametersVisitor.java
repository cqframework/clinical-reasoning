package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Canonicals;
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
    private static final String CRMI_RESOURCE_TYPE_EXTENSION =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-resourceType";
    private static final String CQF_RESOURCE_TYPE_EXTENSION =
            "http://hl7.org/fhir/StructureDefinition/cqf-resourceType";
    private static final String DISPLAY_EXTENSION = "http://hl7.org/fhir/StructureDefinition/display";

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

        // Copy basic metadata
        manifest.setUrl(inputLibrary.getUrl());
        manifest.setVersion(inputLibrary.getVersion());
        manifest.setName(inputLibrary.getName() != null ? inputLibrary.getName() + "Manifest" : "Manifest");
        manifest.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.fromCode(inputLibrary.getStatus()));

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

        // Store parameters as a contained resource
        if (!parameters.getParameter().isEmpty()) {
            parameters.setId("expansion-parameters");
            manifest.getContained().add(parameters);
        }

        return manifest;
    }

    private org.hl7.fhir.r4.model.Library createR4Manifest(ILibraryAdapter inputLibrary) {
        var manifest = new org.hl7.fhir.r4.model.Library();

        // Copy basic metadata
        manifest.setUrl(inputLibrary.getUrl());
        manifest.setVersion(inputLibrary.getVersion());
        manifest.setName(inputLibrary.getName() != null ? inputLibrary.getName() + "Manifest" : "Manifest");
        manifest.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.fromCode(inputLibrary.getStatus()));

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

        // Store parameters as a contained resource
        if (!parameters.getParameter().isEmpty()) {
            parameters.setId("expansion-parameters");
            manifest.getContained().add(parameters);
        }

        return manifest;
    }

    private org.hl7.fhir.r5.model.Library createR5Manifest(ILibraryAdapter inputLibrary) {
        var manifest = new org.hl7.fhir.r5.model.Library();

        // Copy basic metadata
        manifest.setUrl(inputLibrary.getUrl());
        manifest.setVersion(inputLibrary.getVersion());
        manifest.setName(inputLibrary.getName() != null ? inputLibrary.getName() + "Manifest" : "Manifest");
        manifest.setStatus(org.hl7.fhir.r5.model.Enumerations.PublicationStatus.fromCode(inputLibrary.getStatus()));

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

        // Store parameters as a contained resource
        if (!parameters.getParameter().isEmpty()) {
            parameters.setId("expansion-parameters");
            manifest.getContained().add(parameters);
        }

        return manifest;
    }

    private void processRelatedArtifactDstu3(
            ICompositeType relatedArtifact, org.hl7.fhir.dstu3.model.Parameters parameters, ILibraryAdapter library) {
        var canonical = extractCanonical(relatedArtifact, library);
        if (canonical == null) {
            return;
        }

        String resourceType = Canonicals.getResourceType(canonical);
        if (resourceType == null) {
            return;
        }

        // Extract display from relatedArtifact
        String display = extractDisplay(relatedArtifact, library);

        switch (resourceType) {
            case "CodeSystem":
                parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.dstu3.model.StringType(canonical));
                break;
            case "ValueSet":
                var valueSetParam = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.dstu3.model.UriType(canonical));
                // Add cqf-resourceType extension
                valueSetParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.dstu3.model.CodeType(resourceType));
                // Add display extension if present
                if (display != null && !display.isEmpty()) {
                    valueSetParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.dstu3.model.StringType(display));
                }
                break;
            default:
                var param = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.dstu3.model.UriType(canonical));
                // Add crmi-resourceType extension (existing)
                param.addExtension(CRMI_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.dstu3.model.CodeType(resourceType));
                // Add cqf-resourceType extension
                param.addExtension(CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.dstu3.model.CodeType(resourceType));
                // Add display extension if present
                if (display != null && !display.isEmpty()) {
                    param.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.dstu3.model.StringType(display));
                }
                break;
        }
    }

    private void processRelatedArtifactR4(
            ICompositeType relatedArtifact, org.hl7.fhir.r4.model.Parameters parameters, ILibraryAdapter library) {
        var canonical = extractCanonical(relatedArtifact, library);
        if (canonical == null) {
            return;
        }

        String resourceType = Canonicals.getResourceType(canonical);
        if (resourceType == null) {
            return;
        }

        // Extract display from relatedArtifact
        String display = extractDisplay(relatedArtifact, library);

        switch (resourceType) {
            case "CodeSystem":
                parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.r4.model.StringType(canonical));
                break;
            case "ValueSet":
                var valueSetParam = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r4.model.CanonicalType(canonical));
                // Add cqf-resourceType extension
                valueSetParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r4.model.CodeType(resourceType));
                // Add display extension if present
                if (display != null && !display.isEmpty()) {
                    valueSetParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r4.model.StringType(display));
                }
                break;
            default:
                var param = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r4.model.CanonicalType(canonical));
                // Add crmi-resourceType extension (existing)
                param.addExtension(CRMI_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r4.model.CodeType(resourceType));
                // Add cqf-resourceType extension
                param.addExtension(CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r4.model.CodeType(resourceType));
                // Add display extension if present
                if (display != null && !display.isEmpty()) {
                    param.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r4.model.StringType(display));
                }
                break;
        }
    }

    private void processRelatedArtifactR5(
            ICompositeType relatedArtifact, org.hl7.fhir.r5.model.Parameters parameters, ILibraryAdapter library) {
        var canonical = extractCanonical(relatedArtifact, library);
        if (canonical == null) {
            return;
        }

        String resourceType = Canonicals.getResourceType(canonical);
        if (resourceType == null) {
            return;
        }

        // Extract display from relatedArtifact
        String display = extractDisplay(relatedArtifact, library);

        switch (resourceType) {
            case "CodeSystem":
                parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.r5.model.StringType(canonical));
                break;
            case "ValueSet":
                var valueSetParam = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r5.model.CanonicalType(canonical));
                // Add cqf-resourceType extension
                valueSetParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r5.model.CodeType(resourceType));
                // Add display extension if present
                if (display != null && !display.isEmpty()) {
                    valueSetParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r5.model.StringType(display));
                }
                break;
            default:
                var param = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r5.model.CanonicalType(canonical));
                // Add crmi-resourceType extension (existing)
                param.addExtension(CRMI_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r5.model.CodeType(resourceType));
                // Add cqf-resourceType extension
                param.addExtension(CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r5.model.CodeType(resourceType));
                // Add display extension if present
                if (display != null && !display.isEmpty()) {
                    param.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r5.model.StringType(display));
                }
                break;
        }
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

    private String extractDisplay(ICompositeType relatedArtifact, ILibraryAdapter library) {
        var displayPath = library.resolvePath(relatedArtifact, "display");

        if (displayPath instanceof IPrimitiveType<?> primitive) {
            return primitive.getValueAsString();
        }

        return null;
    }
}
