package org.opencds.cqf.cql.evaluator.activitydefinition.r4;

import java.io.InputStream;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class MockFhirDal implements FhirDal {

    private static FhirContext fhirContext;

    private ActivityDefinition loadActivityDefinition(String path) {
        InputStream stream = ActivityDefinitionProcessorTests.class.getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("ActivityDefinition").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s ActivityDefinition", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        return (ActivityDefinition) resource;
    }
    
    @Override
    public IBaseResource read(IIdType id) {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        return loadActivityDefinition("activityDefinition-test.json");
    }

    @Override
    public void create(IBaseResource resource) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(IBaseResource resource) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(IIdType id) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Iterable<IBaseResource> search(String resourceType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
