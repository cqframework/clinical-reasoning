package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.visitor.WithdrawVisitor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class R4WithdrawService {

    private final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    private final IRepository repository;

    public R4WithdrawService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Withdraws an existing artifact if it has status Draft.
     *
     * @param id    the {@link IdType IdType}, always an argument for instance level operations
     * @return  A transaction bundle result of the withdrawn resources
     */
    @Operation(name = "$withdraw", idempotent = true, global = true, type = MetadataResource.class)
    @Description(shortDefinition = "$withdraw", value = "Withdraw an existing draft artifact")
    public Bundle withdraw(@IdParam IdType id) {
        var resource = (MetadataResource) SearchHelper.readRepository(repository, id);
        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }
        var params = new Parameters();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
        try {
            var visitor = new WithdrawVisitor(repository);
            return (Bundle) adapter.accept(visitor, params);
        } catch (Exception e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
    }

}
