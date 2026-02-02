package org.opencds.cqf.fhir.cql;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class TestService {
    FhirContext fhirContext;

    public TestService(FhirContext ctx) {
        fhirContext = ctx;
    }

    public List<IBaseResource> mix(List<IBaseResource> first, List<IBaseResource> second) {
        List<IBaseResource> mixed = new ArrayList<>();
        Set<String> idsOfFirst =
                first.stream().map(r -> r.getIdElement().getValue()).collect(Collectors.toSet());
        for (IBaseResource resource : second) {
            if (!idsOfFirst.contains(resource.getIdElement().getValue())) {
                mixed.add(resource);
            }
        }
        mixed.addAll(first);

        return mixed;
    }
}
