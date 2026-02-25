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
    private static final String CQF_RESOURCE_TYPE_EXTENSION = org.opencds.cqf.fhir.utility.Constants.CQF_RESOURCETYPE;
    private static final String DISPLAY_EXTENSION = org.opencds.cqf.fhir.utility.Constants.DISPLAY_EXTENSION;

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

        // Copy relatedArtifacts from input library to manifest and process for parameters
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            // Add relatedArtifact to manifest
            if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact dstu3RA) {
                manifest.addRelatedArtifact(dstu3RA.copy());
            }
            // Process for expansion parameters
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

        // Copy relatedArtifacts from input library to manifest and process for parameters
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            // Add relatedArtifact to manifest
            if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact r4RA) {
                manifest.addRelatedArtifact(r4RA.copy());
            }
            // Process for expansion parameters
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

        // Copy relatedArtifacts from input library to manifest and process for parameters
        for (var relatedArtifact : inputLibrary.getRelatedArtifact()) {
            // Add relatedArtifact to manifest
            if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact r5RA) {
                manifest.addRelatedArtifact(r5RA.copy());
            }
            // Process for expansion parameters
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

        String resourceType = extractResourceType(relatedArtifact, canonical, library);
        if (resourceType == null) {
            // Default to CodeSystem for unclassifiable depends-on entries.
            // Most unclassifiable entries are external CodeSystems with non-standard URLs
            // (e.g., http://loinc.org, http://www.ada.org/cdt) that lack cqf-resourceType extensions.
            logger.info("Could not determine resource type for '{}', defaulting to CodeSystem", canonical);
            resourceType = "CodeSystem";
        }

        // Extract display from relatedArtifact
        String display = extractDisplay(relatedArtifact, library);

        addParameterDstu3(parameters, resourceType, canonical, display);
    }

    private void addParameterDstu3(
            org.hl7.fhir.dstu3.model.Parameters parameters, String resourceType, String canonical, String display) {
        switch (resourceType) {
            case "CodeSystem":
                var csParam = parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.dstu3.model.StringType(canonical));
                csParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.dstu3.model.CodeType(resourceType));
                if (display != null && !display.isEmpty()) {
                    csParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.dstu3.model.StringType(display));
                }
                break;
            case "ValueSet":
                var valueSetParam = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.dstu3.model.UriType(canonical));
                valueSetParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.dstu3.model.CodeType(resourceType));
                if (display != null && !display.isEmpty()) {
                    valueSetParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.dstu3.model.StringType(display));
                }
                break;
            default:
                var param = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.dstu3.model.UriType(canonical));
                param.addExtension(CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.dstu3.model.CodeType(resourceType));
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

        String resourceType = extractResourceType(relatedArtifact, canonical, library);
        if (resourceType == null) {
            logger.info("Could not determine resource type for '{}', defaulting to CodeSystem", canonical);
            resourceType = "CodeSystem";
        }

        // Extract display from relatedArtifact
        String display = extractDisplay(relatedArtifact, library);

        addParameterR4(parameters, resourceType, canonical, display);
    }

    private void addParameterR4(
            org.hl7.fhir.r4.model.Parameters parameters, String resourceType, String canonical, String display) {
        switch (resourceType) {
            case "CodeSystem":
                var csParam = parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.r4.model.StringType(canonical));
                csParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r4.model.CodeType(resourceType));
                if (display != null && !display.isEmpty()) {
                    csParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r4.model.StringType(display));
                }
                break;
            case "ValueSet":
                var valueSetParam = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r4.model.CanonicalType(canonical));
                valueSetParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r4.model.CodeType(resourceType));
                if (display != null && !display.isEmpty()) {
                    valueSetParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r4.model.StringType(display));
                }
                break;
            default:
                var param = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r4.model.CanonicalType(canonical));
                param.addExtension(CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r4.model.CodeType(resourceType));
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

        String resourceType = extractResourceType(relatedArtifact, canonical, library);
        if (resourceType == null) {
            logger.info("Could not determine resource type for '{}', defaulting to CodeSystem", canonical);
            resourceType = "CodeSystem";
        }

        // Extract display from relatedArtifact
        String display = extractDisplay(relatedArtifact, library);

        addParameterR5(parameters, resourceType, canonical, display);
    }

    private void addParameterR5(
            org.hl7.fhir.r5.model.Parameters parameters, String resourceType, String canonical, String display) {
        switch (resourceType) {
            case "CodeSystem":
                var csParam = parameters
                        .addParameter()
                        .setName("system-version")
                        .setValue(new org.hl7.fhir.r5.model.StringType(canonical));
                csParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r5.model.CodeType(resourceType));
                if (display != null && !display.isEmpty()) {
                    csParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r5.model.StringType(display));
                }
                break;
            case "ValueSet":
                var valueSetParam = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r5.model.CanonicalType(canonical));
                valueSetParam.addExtension(
                        CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r5.model.CodeType(resourceType));
                if (display != null && !display.isEmpty()) {
                    valueSetParam.addExtension(DISPLAY_EXTENSION, new org.hl7.fhir.r5.model.StringType(display));
                }
                break;
            default:
                var param = parameters
                        .addParameter()
                        .setName("canonicalVersion")
                        .setValue(new org.hl7.fhir.r5.model.CanonicalType(canonical));
                param.addExtension(CQF_RESOURCE_TYPE_EXTENSION, new org.hl7.fhir.r5.model.CodeType(resourceType));
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

    /**
     * Extracts the resource type from the cqf-resourceType extension on the relatedArtifact,
     * falling back to inferring from the canonical URL if the extension is not present.
     *
     * @param relatedArtifact the relatedArtifact to extract the resource type from
     * @param canonical the canonical URL to use for fallback inference
     * @param library the library adapter for version-specific path resolution
     * @return the resource type string, or null if it cannot be determined
     */
    private String extractResourceType(ICompositeType relatedArtifact, String canonical, ILibraryAdapter library) {
        // First try to read from cqf-resourceType extension
        // Access extensions directly based on the version-specific type
        if (relatedArtifact instanceof org.hl7.fhir.dstu3.model.RelatedArtifact dstu3Ra) {
            for (var ext : dstu3Ra.getExtension()) {
                if (CQF_RESOURCE_TYPE_EXTENSION.equals(ext.getUrl())
                        && ext.getValue() instanceof IPrimitiveType<?> primitive) {
                    return primitive.getValueAsString();
                }
            }
        } else if (relatedArtifact instanceof org.hl7.fhir.r4.model.RelatedArtifact r4Ra) {
            for (var ext : r4Ra.getExtension()) {
                if (CQF_RESOURCE_TYPE_EXTENSION.equals(ext.getUrl())
                        && ext.getValue() instanceof IPrimitiveType<?> primitive) {
                    return primitive.getValueAsString();
                }
            }
        } else if (relatedArtifact instanceof org.hl7.fhir.r5.model.RelatedArtifact r5Ra) {
            for (var ext : r5Ra.getExtension()) {
                if (CQF_RESOURCE_TYPE_EXTENSION.equals(ext.getUrl())
                        && ext.getValue() instanceof IPrimitiveType<?> primitive) {
                    return primitive.getValueAsString();
                }
            }
        }

        // Fall back to inferring from canonical URL, validating against FHIR resource types
        String inferred = Canonicals.getResourceType(canonical);
        if (inferred != null && fhirContext().getResourceTypes().contains(inferred)) {
            return inferred;
        }
        return null;
    }
}
