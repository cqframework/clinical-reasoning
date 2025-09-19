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
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.visitor.ReleaseVisitor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class R4ReleaseService {

    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    private final IRepository repository;

    public R4ReleaseService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * The release operation supports updating the status of an existing draft artifact to active.
     * The operation sets the date element of the resource and pins versions of all direct and
     * transitive references and records them in the program’s manifest. Child artifacts (i.e.
     * artifacts of which the existing artifact is composed) are also released, recursively.
     * The release operation supports the ability of an authoring repository to transition an
     * artifact and, transitively, any referenced and owned (as indicated by the 'crmiOwned'
     * extension on the RelatedArtifact reference) component artifacts to a released state. The
     * operation SHALL update the status of all owned components to 'active' and update their date
     * to the current date. The operation SHALL ensure that all references for which a version is
     * determined are recorded in the version manifest. For both components and dependencies, if
     * versions are not specified in the relevant reference, the operation will look up the version
     * to be used in the version manifest.
     * When 'requireVersionSpecificReferences' is true then all references SHALL either be
     * version-specific or, if they are not, an entry SHALL exist in the version manifest to specify
     * which version of the referenced resource should be used. If,
     * 'requireVersionSpecificReferences' is true and there exists a reference that is not
     * version-specific and no entry exists in the version manifest for the referenced resource, the
     * program is considered to be in an invalid state and not eligible for release. If
     * 'requireVersionSpecificReferences' is false (the default), then unversioned references are
     * valid and the artifact can be released in that state - deferring the version determination to
     * the consumer.
     * When 'requireActiveReferences' is true then the operation SHALL throw an error if any 'draft'
     * or 'retired' dependencies are found.
     * @param id                    The logical id of the artifact to release. The server must know
     *                              the artifact (e.g. it is defined explicitly in the server's
     *                              artifacts)
     * @param version               Specifies the version to be applied—based on the version
     *                              behavior specified—to the artifact being released and any
     *                              referenced owned components.
     * @param versionBehavior       Indicates the behavior with which the 'version' parameter
     *                              should apply to the artifact being released and its components.
     * @param latestFromTxServer    Indicates whether the terminology server from which a value set
     *                              was originally downloaded should be checked for the latest
     *                              version. The terminology server of origin is tracked via the
     *                              <a href="https://hl7.org/fhir/extension-valueset-authoritativesource.html">authoritativeSource</a>
     *                              extension on the value set. If this flag is set to false or the
     *                              value set does not have an authoritativeSource specified, then
     *                              the check should be constrained to the local system/cache.
     *                              (default = false)
     * @param releaseLabel          Specifies a release label to be applied to the artifact(s)
     *                              being released
     * @return  The Bundle result containing the released resource(s)
     */
    public Bundle release(
            IdType id,
            String version,
            CodeType versionBehavior,
            IPrimitiveType<Boolean> latestFromTxServer,
            CodeType requireNonExperimental,
            Endpoint terminologyEndpoint,
            String releaseLabel)
            throws FHIRException {
        var resource = (MetadataResource) SearchHelper.readRepository(repository, id);
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
        if (requireNonExperimental != null) {
            params.addParameter("requireNonExperimental", requireNonExperimental);
        }
        if (releaseLabel != null) {
            params.addParameter("releaseLabel", releaseLabel);
        }
        if (terminologyEndpoint != null) {
            params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        }
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
        var visitor = new ReleaseVisitor(repository);
        //            TODO: Create an ERSD processor to handle this
        //            adapter.getRelatedArtifact()
        //                .forEach(ra -> {
        //                    checkIfValueSetNeedsCondition(null, (RelatedArtifact) ra, repository); // ERSD Specific -
        // need on client-side
        //                });
        return (Bundle) adapter.accept(visitor, params);
    }
}
