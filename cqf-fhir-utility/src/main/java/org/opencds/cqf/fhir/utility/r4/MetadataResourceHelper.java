package org.opencds.cqf.fhir.utility.r4;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;

public class MetadataResourceHelper {
    public static void forEachMetadataResource(
            List<BundleEntryComponent> entries, Consumer<MetadataResource> callback, Repository repository) {
        entries.stream()
                .map(entry -> entry.getResponse().getLocation())
                .map(location -> {
                    switch (location.split("/")[0]) {
                        case "ActivityDefinition":
                            return repository.read(ActivityDefinition.class, new IdType(location));
                        case "Library":
                            return repository.read(Library.class, new IdType(location));
                        case "Measure":
                            return repository.read(Measure.class, new IdType(location));
                        case "PlanDefinition":
                            return repository.read(PlanDefinition.class, new IdType(location));
                        case "ValueSet":
                            return repository.read(ValueSet.class, new IdType(location));
                        default:
                            return null;
                    }
                })
                .forEach(callback);
    }

    public static <T extends Type> Optional<T> getParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameter(name))
                .map(p -> factory.createParametersParameters(p))
                .map(rl -> (T) rl.getValue());
    }

    public static <T extends IBaseResource> Optional<T> getResourceParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameter(name))
                .map(p -> factory.createParametersParameters(p))
                .map(rl -> (T) rl.getResource());
    }

    public static <T extends Type> Optional<List<T>> getListParameter(
            String name, IBaseParameters operationParameters, Class<T> type) {
        var factory = AdapterFactory.forFhirVersion(operationParameters.getStructureFhirVersionEnum());
        return Optional.ofNullable(operationParameters)
                .map(p -> factory.createParameters(p))
                .map(p -> p.getParameterValues(name))
                .map(vals -> vals.stream().map(rl -> (T) rl).collect(Collectors.toList()));
    }

    public static List<MetadataResource> getMetadataResourcesFromBundle(Bundle bundle) {
        List<MetadataResource> resourceList = new ArrayList<>();

        if (!bundle.getEntryFirstRep().isEmpty()) {
            List<Bundle.BundleEntryComponent> referencedResourceEntries = bundle.getEntry();
            for (Bundle.BundleEntryComponent entry : referencedResourceEntries) {
                if (entry.hasResource() && entry.getResource() instanceof MetadataResource) {
                    MetadataResource referencedResource = (MetadataResource) entry.getResource();
                    resourceList.add(referencedResource);
                }
            }
        }

        return resourceList;
    }
}
