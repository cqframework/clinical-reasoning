package org.opencds.cqf.fhir.cr.graphdefintion.apply;

import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class GraphDefinitionProcessor {

    protected IRepository repository;

    public GraphDefinitionProcessor(IRepository repository) {
        this.repository = repository;
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters apply(
            Either3<C, IIdType, R> graphDefinition,
            @Nullable String subject,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            IBaseParameters parameters) {

        return newParameters(repository.fhirContext());
    }
}
