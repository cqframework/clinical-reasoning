package org.opencds.cqf.fhir.cr.questionnaire.populate;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.utility.repository.INpmRepository;

public class ResourceResolverWithNpmBacking extends ResourceResolver {

    private final INpmRepository npmRepository;

    public ResourceResolverWithNpmBacking(String resourceType, IRepository repository, INpmRepository npmRepository) {
        super(resourceType, repository);
        this.npmRepository = npmRepository;
    }

    @Override
    protected <C extends IPrimitiveType<String>> IBaseResource resolveByUrl(C url) {
        // check the repository first
        IBaseResource resource = super.resolveByUrl(url);

        // if not found, check the npm repository
        if (resource == null) {
            List<? extends IBaseResource> resources = npmRepository.resolveByUrl(clazz, url.getValue());
            // we only take the first one because this method only expects
            // a single one to be returned
            // sorting can be implemented to have the most recent version first
            if (resources != null && !resources.isEmpty()) {
                resource = resources.stream().findFirst().get();
            }
        }
        return resource;
    }
}
