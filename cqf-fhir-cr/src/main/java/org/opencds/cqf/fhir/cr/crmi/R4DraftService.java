package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.visitor.DraftVisitor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class R4DraftService {

    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    private final IRepository repository;

    public R4DraftService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a draft version of a knowledge artifact and all its children.
     * This operation is used to set the status and version. It also removes effectivePeriod,
     * approvalDate and any extensions which are only valid for active artifacts.
     *
     * @param id                The logical id of the artifact to draft. The server must know the
     *                          artifact (e.g. it is defined explicitly in the server's resources)
     * @param version           A semantic version in the form MAJOR.MINOR.PATCH.REVISION
     * @return  The {@link Bundle Bundle} result containing the new resource(s). If inputParameters
     *          are present in the manifest being drafted, those parameters are moved to the
     *          expansionParameters extension in the new draft.
     */
    public Bundle draft(@IdParam IdType id, @OperationParam(name = "version") String version) throws FHIRException {
        var resource = (MetadataResource) SearchHelper.readRepository(repository, id);
        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }
        var params = new Parameters().addParameter("version", new StringType(version));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
        var visitor = new DraftVisitor(repository);
        return ((Bundle) adapter.accept(visitor, params));
    }
}
