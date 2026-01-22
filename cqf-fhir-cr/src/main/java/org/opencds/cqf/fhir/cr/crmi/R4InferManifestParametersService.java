package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.cr.visitor.InferManifestParametersVisitor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class R4InferManifestParametersService {

    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    private final IRepository repository;

    public R4InferManifestParametersService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Infers manifest expansion parameters from a module-definition Library.
     * This operation takes a module-definition Library (output of $data-requirements)
     * and converts its relatedArtifacts into manifest expansion parameters following
     * CRMI conventions.
     *
     * @param id  The logical id of the module-definition Library to process
     * @return    The {@link Library Library} result (asset-collection manifest with
     *            expansion parameters)
     */
    public Library inferManifestParameters(@IdParam IdType id) throws FHIRException {
        var resource = (Library) SearchHelper.readRepository(repository, id);
        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }
        return inferManifestParameters(resource);
    }

    /**
     * Infers manifest expansion parameters from a module-definition Library.
     * This operation takes a module-definition Library (output of $data-requirements)
     * and converts its relatedArtifacts into manifest expansion parameters following
     * CRMI conventions.
     *
     * @param library  The module-definition Library to process
     * @return         The {@link Library Library} result (asset-collection manifest with
     *                 expansion parameters)
     */
    public Library inferManifestParameters(Library library) throws FHIRException {
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
        var visitor = new InferManifestParametersVisitor(repository);
        return (Library) adapter.accept(visitor, null);
    }
}
