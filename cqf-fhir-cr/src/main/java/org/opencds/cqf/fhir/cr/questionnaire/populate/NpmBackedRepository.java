package org.opencds.cqf.fhir.cr.questionnaire.populate;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.jetbrains.annotations.NotNull;
import org.opencds.cqf.fhir.utility.KnowledgeArtifactUtil;
import org.opencds.cqf.fhir.utility.repository.INpmComboRepository;
import org.opencds.cqf.fhir.utility.repository.INpmRepository;

public class NpmBackedRepository implements INpmComboRepository {

    private final IRepository repository;
    private final INpmRepository npmRepository;

    public NpmBackedRepository(@Nonnull IRepository baseRepository, @Nonnull INpmRepository npmRepository) {
        this.repository = baseRepository;
        this.npmRepository = npmRepository;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        return repository.read(resourceType, id, headers);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {

        B bundle = repository.search(bundleType, resourceType, searchParameters, headers);

        String rt = fhirContext().getResourceType(resourceType);

        // if this isn't a knowledge resource type, we'll won't check the backing IG
        // (because backing IG would only contain knowledge resources)
        if (!KnowledgeArtifactUtil.RESOURCE_TYPES.contains(rt)) {
            return bundle;
        }

        // TODO - this is inelegant; only check the npm for the various actual questionnaire etc resources
        // bundle.isEmpty() means nothing, so we have to check
        // if there are actually results in it by retrieving the actual results first
        List<IBaseResource> resources = BundleUtil.toListOfResources(fhirContext(), bundle);
        if (resources != null && !resources.isEmpty()) {
            return bundle;
        }

        List<T> allTypes = npmRepository.resolveByUrl(resourceType, null);

        BundleBuilder bundleBuilder = new BundleBuilder(fhirContext());
        bundleBuilder.setType("collection");
        for (T entry : allTypes) {
            bundleBuilder.addCollectionEntry(entry);
        }

        return (B) bundleBuilder.getBundle();
    }

    @Override
    public @NotNull FhirContext fhirContext() {
        return repository.fhirContext();
    }

    @Override
    public <T extends IBaseResource> List<T> resolveByUrl(Class<T> clazz, String url) {
        return npmRepository.resolveByUrl(clazz, url);
    }
}
