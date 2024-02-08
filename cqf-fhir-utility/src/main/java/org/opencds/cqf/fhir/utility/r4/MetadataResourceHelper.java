package org.opencds.cqf.fhir.utility.r4;

import java.util.List;
import java.util.function.Consumer;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;

public class MetadataResourceHelper {
    public static void forEachMetadataResource(List<BundleEntryComponent> entries, Consumer<MetadataResource> callback, Repository theRepository) {
		entries.stream()
			.map(entry -> entry.getResponse().getLocation())
			.map(location -> {
				switch (location.split("/")[0]) {
					case "ActivityDefinition":
						return theRepository.read(ActivityDefinition.class, new IdType(location));
					case "Library":
						return theRepository.read(Library.class, new IdType(location));
					case "Measure":
						return theRepository.read(Measure.class, new IdType(location));
					case "PlanDefinition":
						return theRepository.read(PlanDefinition.class, new IdType(location));
					case "ValueSet":
						return theRepository.read(ValueSet.class, new IdType(location));
					default:
						return  null;
				}
			})
			.forEach(callback);
	}
}
