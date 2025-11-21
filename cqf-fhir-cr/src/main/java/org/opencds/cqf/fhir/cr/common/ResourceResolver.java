package org.opencds.cqf.fhir.cr.common;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Resources.castOrThrow;

import ca.uhn.fhir.repository.IRepository;
import java.util.function.Function;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.monad.Either;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.search.Searches;

public class ResourceResolver {
    final String invalidResourceType = "The resource passed in was not a valid instance of %s.class";
    final String resourceType;
    final IRepository repository;
    protected final Class<? extends IBaseResource> clazz;
    final Class<? extends IBaseBundle> bundleClazz;

    @SuppressWarnings("unchecked")
    public ResourceResolver(String resourceType, IRepository repository) {
        this.resourceType = resourceType;
        this.repository = repository;
        try {
            switch (this.repository.fhirContext().getVersion().getVersion()) {
                case DSTU3:
                    clazz = (Class<? extends IBaseResource>) Class.forName("org.hl7.fhir.dstu3.model." + resourceType);
                    bundleClazz = org.hl7.fhir.dstu3.model.Bundle.class;
                    break;
                case R4:
                    clazz = (Class<? extends IBaseResource>) Class.forName("org.hl7.fhir.r4.model." + resourceType);
                    bundleClazz = org.hl7.fhir.r4.model.Bundle.class;
                    break;
                case R5:
                    clazz = (Class<? extends IBaseResource>) Class.forName("org.hl7.fhir.r5.model." + resourceType);
                    bundleClazz = org.hl7.fhir.r5.model.Bundle.class;
                    break;
                default:
                    throw new AssertionError();
            }
        } catch (ClassNotFoundException e) {
            throw new AssertionError();
        }
    }

    protected <C extends IPrimitiveType<String>> IBaseResource resolveByUrl(C url) {
        var result = this.repository.search(bundleClazz, clazz, Searches.byCanonical(url.getValue()));
        var iterator = new BundleMappingIterable<>(repository, result, p -> p.getResource()).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    protected IBaseResource resolveById(IIdType id) {
        return this.repository.read(clazz, id);
    }

    @SuppressWarnings("unchecked")
    public <C extends IPrimitiveType<String>, T extends IBaseResource> T resolve(Either3<C, IIdType, T> resource)
            throws FHIRException {
        var baseResource = resource.fold(this::resolveByUrl, this::resolveById, Function.identity());

        requireNonNull(baseResource, "Unable to resolve %s".formatted(resourceType));

        return (T) castOrThrow(baseResource, clazz, invalidResourceType.formatted(resourceType))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T resolve(Either<IIdType, T> resource) {
        var baseResource = resource.fold(this::resolveById, Function.identity());

        requireNonNull(baseResource, "Unable to resolve %s".formatted(resourceType));

        return (T) castOrThrow(baseResource, clazz, invalidResourceType.formatted(resourceType))
                .orElse(null);
    }
}
