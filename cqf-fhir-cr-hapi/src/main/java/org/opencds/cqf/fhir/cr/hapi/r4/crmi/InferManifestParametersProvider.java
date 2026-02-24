package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.cr.hapi.r4.IInferManifestParametersServiceFactory;

public class InferManifestParametersProvider {

    private final IInferManifestParametersServiceFactory r4InferManifestParametersServiceFactory;

    public InferManifestParametersProvider(
            IInferManifestParametersServiceFactory r4InferManifestParametersServiceFactory) {
        this.r4InferManifestParametersServiceFactory = r4InferManifestParametersServiceFactory;
    }

    /**
     * Infers manifest expansion parameters from a module-definition Library.
     * This operation takes a module-definition Library (output of $data-requirements)
     * and converts its relatedArtifacts into manifest expansion parameters following
     * CRMI conventions:
     * <ul>
     *   <li>CodeSystem → system-version parameter (format: "system|version")</li>
     *   <li>ValueSet → canonicalVersion parameter</li>
     *   <li>Other resources → canonicalVersion parameter with resourceType extension</li>
     * </ul>
     *
     * @param id              The logical id of the module-definition Library to process
     * @param requestDetails  The {@link RequestDetails RequestDetails}
     * @return                The {@link Library Library} result (asset-collection manifest
     *                        with expansion parameters as a contained Parameters resource)
     */
    @Operation(
            name = "$infer-manifest-parameters",
            idempotent = true,
            type = Library.class,
            canonicalUrl = "http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-infer-manifest-parameters")
    @Description(
            shortDefinition = "$infer-manifest-parameters",
            value = "Infer manifest expansion parameters from a module-definition Library.")
    public Library inferManifestParameters(@IdParam IdType id, RequestDetails requestDetails) {
        return r4InferManifestParametersServiceFactory.create(requestDetails).inferManifestParameters(id);
    }

    /**
     * Infers manifest expansion parameters from a module-definition Library.
     * This operation takes a module-definition Library (output of $data-requirements)
     * and converts its relatedArtifacts into manifest expansion parameters following
     * CRMI conventions:
     * <ul>
     *   <li>CodeSystem → system-version parameter (format: "system|version")</li>
     *   <li>ValueSet → canonicalVersion parameter</li>
     *   <li>Other resources → canonicalVersion parameter with resourceType extension</li>
     * </ul>
     *
     * @param library         The module-definition Library to process
     * @param requestDetails  The {@link RequestDetails RequestDetails}
     * @return                The {@link Library Library} result (asset-collection manifest
     *                        with expansion parameters as a contained Parameters resource)
     */
    @Operation(
            name = "$infer-manifest-parameters",
            idempotent = true,
            type = Library.class,
            canonicalUrl = "http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-infer-manifest-parameters")
    @Description(
            shortDefinition = "$infer-manifest-parameters",
            value = "Infer manifest expansion parameters from a module-definition Library.")
    public Library inferManifestParameters(
            @OperationParam(name = "library") Library library, RequestDetails requestDetails) {
        return r4InferManifestParametersServiceFactory.create(requestDetails).inferManifestParameters(library);
    }
}
