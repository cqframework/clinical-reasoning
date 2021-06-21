package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class MockFhirDal implements FhirDal {

    private static FhirContext fhirContext;

    private PlanDefinition loadPlanDefinition(String path) {
        InputStream stream = PlanDefinitionProcessorTests.class.getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("PlanDefinition").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s PlanDefinition", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        return (PlanDefinition) resource;
    }

    private Encounter loadEncounter(String path) {
        InputStream stream = PlanDefinitionProcessorTests.class.getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Encounter").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Encounter", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        return (Encounter) resource;
    }

    private Iterable<IBaseResource> loadResourceWithURLFromBundle(String path, String url) {
        InputStream stream = PlanDefinitionProcessorTests.class.getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Bundle", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        Bundle bundle = (Bundle) resource;
        List<IBaseResource> resources = new ArrayList<IBaseResource>();
        bundle.getEntry().forEach(entry -> {
            if (entry.getResource() instanceof ActivityDefinition) {
                if (url.startsWith("ActivityDefinition/")) {
                    String id = url.substring(url.lastIndexOf("/") + 1, url.length());
                    if (entry.getResource().getIdElement().getIdPart().equals(id)) {
                        resources.add(entry.getResource());
                    }
                }
                else if (((ActivityDefinition)entry.getResource()).getUrl().equals(url)) {
                    resources.add(entry.getResource());
                }
            }
        });
        bundle.getEntry().forEach(entry -> {
            if (entry.getResource() instanceof ValueSet) {
                if (url.startsWith("ValueSet/")) {
                    String id = url.substring(url.lastIndexOf("/") + 1, url.length());
                    if (entry.getResource().getIdElement().getIdPart().equals(id)) {
                        resources.add(entry.getResource());
                    }
                }
                else if (((ValueSet)entry.getResource()).getUrl().equals(url)) {
                    resources.add(entry.getResource());
                }
            }
        });

        return resources;
    }

    private Iterable<IBaseResource> searchForResourcesInBundle(String path, String resourceType) {
        InputStream stream = PlanDefinitionProcessorTests.class.getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Bundle", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        Bundle bundle = (Bundle) resource;
        List<IBaseResource> resources = new ArrayList<IBaseResource>();
        getResourcesFromBundle(resourceType, bundle, resources);

        return resources;
    }

    private void getResourcesFromBundle(String resourceType, Bundle bundle, List<IBaseResource> resources) {
        bundle.getEntry().forEach(entry -> {
            if (entry.getResource() instanceof Bundle) {
                getResourcesFromBundle(resourceType, (Bundle) entry.getResource(), resources);
            }
            if (entry.getResource().getResourceType().toString().equals(resourceType)) {
                resources.add(entry.getResource());
            }
        });
    }
    
    @Override
    public IBaseResource read(IIdType id) {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        if (id.hasResourceType() && id.getResourceType().equals("PlanDefinition")) {
            return loadPlanDefinition("plandefinition-RuleFilters-1.0.0.json");
        }
        else if (id.hasResourceType() && id.getResourceType().equals("ActivityDefinition")) {
            Iterator<IBaseResource> resources = loadResourceWithURLFromBundle("RuleFilters-1.0.0-bundle.json", "ActivityDefinition/" + id.getIdPart()).iterator();
            IBaseResource resource = resources.next();
            return resource;
        } else if (id.hasResourceType() && id.getResourceType().equals("Encounter")) {
            return loadEncounter("reportable-encounter.json");
        } else {
            return loadPlanDefinition("plandefinition-RuleFilters-1.0.0.json");
        }
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
        return searchForResourcesInBundle("RuleFilters-1.0.0-bundle.json", resourceType);
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        return loadResourceWithURLFromBundle("RuleFilters-1.0.0-bundle.json", url);
    }
    
}
