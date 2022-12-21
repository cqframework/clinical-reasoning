package org.opencds.cqf.cql.evaluator.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.measure.FhirResourceLoader;
import org.opencds.cqf.fhir.api.Repository;

import java.util.*;

public class R4TestRepository implements Repository {

    public static void main(String[] args) {
        R4TestRepository repository = new R4TestRepository();
        IBaseResource res = repository.read(Patient.class, new IdType("example"), null);
        System.out.println(res.getIdElement().getIdPart());

        Patient john = new Patient();
        john.setId(new IdType("Patient", "id-john-doe"));
        repository.create(john, null);

        repository.create(john, null);

        IBaseBundle bundle = repository.search(Bundle.class, Patient.class, null, null);
        System.out.println(((Bundle) bundle).getEntry().size());
    }

    FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);
    private Map<IdType, IBaseResource> resourceMap;

    public R4TestRepository() {
        FhirResourceLoader fhirResourceLoader = new FhirResourceLoader(context, this.getClass(),
                List.of("res/tests", "res/vocabulary/CodeSystem/", "res/vocabulary/ValueSet/", "res/content/"),
                false);
        List<IBaseResource> list = fhirResourceLoader.getResources();
        System.out.println(list.size());

        resourceMap = new LinkedHashMap<>();

        list.forEach(resource -> {
            resourceMap.put(new IdType(resource.getIdElement().getResourceType(), resource.getIdElement().getIdPart()), resource);
        });

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource, I extends IIdType> T read(Class<T> resourceType, I id, Map<String, String> headers) {
        IdType theId = new IdType(resourceType.getSimpleName(), id.getIdPart());
        if (resourceMap.containsKey(theId)) {
            return (T) resourceMap.get(theId);
        }
        throw new ResourceNotFoundException("Resource not found with id " + theId);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        MethodOutcome methodOutcome = new MethodOutcome();

        if(IBaseResource.class.isAssignableFrom(resource.getClass())) {

            IBaseResource iBaseResource = (IBaseResource) resource;

            IdType theId = new IdType(iBaseResource.getIdElement().getResourceType(), iBaseResource.getIdElement().getIdPart());
            if(!resourceMap.containsKey(theId)) {
                resourceMap.put(theId, iBaseResource);
                methodOutcome.setCreated(true);
                System.out.println("created:" + theId);
            } else {
                Long nextLong = new Random().nextLong();
                theId = new IdType(iBaseResource.getIdElement().getResourceType(),
                        iBaseResource.getIdElement().getIdPart(),
                        nextLong.toString());
                resourceMap.put(theId, iBaseResource);
                methodOutcome.setCreated(true);
                System.out.println("created:" + theId);
            }

        } else {
            // IBaseResource.class is not super class of resource
            methodOutcome.setStatusCode(400);
        }
        return methodOutcome;
    }

    @Override
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(I id, P patchParameters, Map<String, String> headers) {
        return null;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        MethodOutcome methodOutcome = new MethodOutcome();

        if(IBaseResource.class.isAssignableFrom(resource.getClass())) {

            IBaseResource iBaseResource = (IBaseResource) resource;

            IdType theId = new IdType(iBaseResource.getIdElement().getResourceType(), iBaseResource.getIdElement().getIdPart());
            if(resourceMap.containsKey(theId)) {
                resourceMap.put(theId, iBaseResource);
                System.out.println("updated:" + theId);
            } else {
                resourceMap.put(theId, iBaseResource);
                methodOutcome.setCreated(true);
                System.out.println("created:" + theId);
            }
        } else {
            // IBaseResource.class is not super class of resource
            methodOutcome.setStatusCode(400);
        }
        return methodOutcome;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(Class<T> resourceType, I id, Map<String, String> headers) {
        MethodOutcome methodOutcome = new MethodOutcome();

        IdType theId = new IdType(resourceType.getSimpleName(), id.getIdPart());

        if (resourceMap.containsKey(theId)) {
            resourceMap.remove(theId);
        } else {
         throw new ResourceNotFoundException("Resource not found with id " + theId);
        }
        return methodOutcome;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType,
                                                                     Class<T> resourceType,
                                                                     Map<String, List<IQueryParameterType>> searchParameters,
                                                                     Map<String, String> headers) {
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.SEARCHSET);

        for(IBaseResource resource : resourceMap.values()) {
            if(resource.getIdElement().getResourceType().equals(resourceType.getSimpleName())) {
                bundle.addEntry(new Bundle.BundleEntryComponent().setResource((Resource) resource));
            }
        }

        return (B) bundle;
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        return null;
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers) {
        return null;
    }


    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(I id, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(I id, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }
}
