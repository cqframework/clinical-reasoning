package org.opencds.cqf.fhir.cql;

import static ca.uhn.fhir.util.ElementUtil.isEmpty;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class MyNewService {

    FhirContext ctx;

    public MyNewService(FhirContext ctx) {
        this.ctx = ctx;
    }

    public IBaseResource runSystem(String resourceName) {
        if (isEmpty(resourceName)) {
            throw new RuntimeException("This is an exception");
        }

        var resourceDefinition = ctx.getResourceDefinition(resourceName);

        return resourceDefinition.newInstance();
    }

    public void sortMyLists(List<IBaseResource> resource1, List<IBaseResource> resource2) {
        List<IBaseResource> newRe = new ArrayList<>();
        for (IBaseResource resource : resource1) {
            if (!resource2.contains(resource)) {
                newRe.add(resource);
            }
        }
        resource1.addAll(newRe);
    }
}
