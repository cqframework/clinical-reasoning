package org.opencds.cqf.fhir.utility.r5;

import java.util.List;
import java.util.function.Consumer;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;

public class MetadataResourceHelper {
    public static void forEachMetadataResource(
            List<BundleEntryComponent> entries,
            Consumer<org.hl7.fhir.r5.model.MetadataResource> callback,
            Repository repository) {
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
}
