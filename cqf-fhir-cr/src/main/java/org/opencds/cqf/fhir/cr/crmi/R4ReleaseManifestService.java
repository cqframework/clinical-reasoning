package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.visitor.ReleaseManifestVisitor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

/**
 * Service for the $release-manifest operation. Operates on manifest Libraries (asset-collection)
 * that have pre-computed depends-on entries from $infer-manifest-parameters.
 */
public class R4ReleaseManifestService {

    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    private final IRepository repository;

    public R4ReleaseManifestService(IRepository repository) {
        this.repository = repository;
    }

    public Bundle releaseManifest(
            IdType id,
            String version,
            CodeType versionBehavior,
            IPrimitiveType<Boolean> latestFromTxServer,
            Endpoint terminologyEndpoint,
            String releaseLabel)
            throws FHIRException {
        var resource = (Library) SearchHelper.readRepository(repository, id);
        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }
        var params = new Parameters();
        if (version != null) {
            params.addParameter("version", version);
        }
        if (versionBehavior != null) {
            params.addParameter("versionBehavior", versionBehavior);
        }
        if (latestFromTxServer != null && latestFromTxServer.hasValue()) {
            params.addParameter("latestFromTxServer", latestFromTxServer.getValue());
        }
        if (releaseLabel != null) {
            params.addParameter("releaseLabel", releaseLabel);
        }
        if (terminologyEndpoint != null) {
            params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        }
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
        var visitor = new ReleaseManifestVisitor(repository);
        return (Bundle) adapter.accept(visitor, params);
    }
}
