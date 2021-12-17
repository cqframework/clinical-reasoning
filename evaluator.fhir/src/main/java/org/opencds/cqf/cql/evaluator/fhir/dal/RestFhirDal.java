package org.opencds.cqf.cql.evaluator.fhir.dal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;

public class RestFhirDal implements FhirDal {

    private IGenericClient client;

    public RestFhirDal(IGenericClient client) {
        this.client = client;
    }

    @Override
    public IBaseResource read(IIdType id) {
        return this.client.read().resource(id.getResourceType()).withId(id).execute();
    }

    @Override
    public void create(IBaseResource resource) {
        this.client.create().resource(resource).execute();
    }

    @Override
    public void update(IBaseResource resource) {
        this.client.update().resource(resource).execute();
    }

    @Override
    public void delete(IIdType id) {
        this.client.delete().resourceById(id).execute();
    }

    @Override
    public Iterable<IBaseResource> search(String resourceType) {
        IBaseBundle bundle = this.client.search().forResource(resourceType).execute();
        return BundleUtil.toListOfResources(this.client.getFhirContext(), bundle);
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        Map<String, List<String>> params = new HashMap<>();
        params.put("url", Arrays.asList(url));
        IBaseBundle bundle = this.client.search().forResource(resourceType).whereMap(params).execute();
        return BundleUtil.toListOfResources(this.client.getFhirContext(), bundle);
    }
    
}
