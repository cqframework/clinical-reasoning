package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class PropertyHelper {
    Repository repository;
    BaseRuntimeElementDefinition getPropertyDefinition(Property property) {
        return repository.fhirContext().getElementDefinition(
            property.getTypeCode().contains("|")
                ? property.getTypeCode().split("\\|")[0]
                : property.getTypeCode());
    }

    Property getSubjectProperty(Resource resource) {
        final Property subjectProperty = resource.getNamedProperty(NamedProperties.SUBJECT);
        if (subjectProperty == null) {
            return resource.getNamedProperty(NamedProperties.PATIENT);
        }
        return subjectProperty;
    }

    Property getAuthorProperty(Resource resource) {
        final Property property = resource.getNamedProperty(NamedProperties.RECORDER);
        if (property == null && resource.fhirType().equals(FHIRAllTypes.OBSERVATION.toCode())) {
            return resource.getNamedProperty(NamedProperties.PERFORMER);
        }
        return property;
    }

    List<Property> getDateProperties(Resource resource) {
        final List<Property> results = new ArrayList<>();
        results.add(resource.getNamedProperty(NamedProperties.ONSET));
        results.add(resource.getNamedProperty(NamedProperties.ISSUED));
        results.add(resource.getNamedProperty(NamedProperties.EFFECTIVE));
        results.add(resource.getNamedProperty(NamedProperties.RECORD_DATE));
        return results.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
