package org.opencds.cqf.fhir.cr.activitydefinition;

import static java.util.Objects.requireNonNull;
import java.util.function.Function;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import static org.opencds.cqf.fhir.utility.Resources.castOrThrow;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.search.Searches;

public class ResolveProcessor {
    final Repository repository;
    final Class<? extends IBaseResource> clazz;
    final Class<? extends IBaseBundle> bundleClazz;

    public ResolveProcessor(Repository repository) {
        this.repository = repository;
        switch (this.repository.fhirContext().getVersion().getVersion()) {
            case DSTU3:
                clazz = org.hl7.fhir.dstu3.model.ActivityDefinition.class;
                bundleClazz = org.hl7.fhir.dstu3.model.Bundle.class;
                break;
            case R4:
                clazz = org.hl7.fhir.r4.model.ActivityDefinition.class;
                bundleClazz = org.hl7.fhir.r4.model.Bundle.class;
                break;
            case R5:
                clazz = org.hl7.fhir.r5.model.ActivityDefinition.class;
                bundleClazz = org.hl7.fhir.r5.model.Bundle.class;
                break;
            default:
                throw new AssertionError();
        }
    }

    protected <C extends IPrimitiveType<String>> IBaseResource resolveByUrl(C url) {
        var parts = Canonicals.getParts(url);
        var result = this.repository.search(
                bundleClazz, clazz, Searches.byNameAndVersion(parts.idPart(), parts.version()));
        var iterator = new BundleMappingIterable<>(repository, result, p -> p.getResource()).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    protected IBaseResource resolveById(IIdType id) {
        return this.repository.read(clazz, id);
    }

    @SuppressWarnings("unchecked")
    public <C extends IPrimitiveType<String>, T extends IBaseResource> T resolveActivityDefinition(
            Either3<C, IIdType, T> activityDefinition) throws FHIRException {
        var baseActivityDefinition = activityDefinition.fold(this::resolveByUrl, this::resolveById, Function.identity());

        requireNonNull(baseActivityDefinition, "Unable to resolve ActivityDefinition");

        return (T) castOrThrow(
                        baseActivityDefinition,
                        clazz,
                        "The activityDefinition passed in was not a valid instance of ActivityDefinition.class")
                .orElse(null);
    }
    
}
