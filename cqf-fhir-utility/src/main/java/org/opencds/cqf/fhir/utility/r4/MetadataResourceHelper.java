package org.opencds.cqf.fhir.utility.r4;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import java.util.function.Consumer;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;

public class MetadataResourceHelper {
    private MetadataResourceHelper() {}

    public static void forEachMetadataResource(
            List<BundleEntryComponent> entries,
            Consumer<org.hl7.fhir.r4.model.MetadataResource> callback,
            IRepository repository) {
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
