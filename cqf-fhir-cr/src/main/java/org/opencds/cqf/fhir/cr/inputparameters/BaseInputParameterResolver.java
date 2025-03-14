package org.opencds.cqf.fhir.cr.inputparameters;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

public abstract class BaseInputParameterResolver implements IInputParameterResolver {
    protected final IIdType subjectId;
    protected final IIdType encounterId;
    protected final IIdType practitionerId;
    protected Repository repository;

    protected BaseInputParameterResolver(
            Repository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            boolean useServerData,
            IBaseBundle data) {
        this.subjectId = subjectId;
        this.encounterId = encounterId;
        this.practitionerId = practitionerId;
        Repository bundleRepository = null;
        if (data != null) {
            bundleRepository = new InMemoryFhirRepository(repository.fhirContext(), data);
        }
        this.repository = resolveRepository(useServerData, repository, bundleRepository);
    }

    protected final Repository resolveRepository(
            Boolean useServerData, Repository serverRepository, Repository bundleRepository) {
        if (bundleRepository == null) {
            return serverRepository;
        } else {
            return Boolean.TRUE.equals(useServerData)
                    ? new FederatedRepository(serverRepository, bundleRepository)
                    : bundleRepository;
        }
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }

    protected <R extends IBaseResource> R readRepository(Class<R> resourceType, IIdType id) {
        try {
            return repository.read(resourceType, id);
        } catch (Exception e) {
            return null;
        }
    }

    protected abstract IBaseParameters resolveParameters(
            IBaseParameters parameters,
            List<IParametersParameterComponentAdapter> context,
            List<IBaseExtension<?, ?>> launchContext);
}
