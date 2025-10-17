package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.visitor.RetireVisitor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class R4RetireService {

    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    private final IRepository repository;

    public R4RetireService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Retires an existing artifact if it has status Draft.
     *
     * @param id the {@link IdType IdType}, always an argument for instance level operations
     * @return A transaction {@link Bundle Bundle} result of the retired resources
     */
    public Bundle retire(@IdParam IdType id) throws FHIRException {
        var resource = (MetadataResource) SearchHelper.readRepository(repository, id);
        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }
        var params = new Parameters();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
        try {
            var visitor = new RetireVisitor(repository);
            return (Bundle) adapter.accept(visitor, params);
        } catch (Exception e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
    }
}
