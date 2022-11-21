package org.opencds.cqf.cql.evaluator.fhir.dal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.BundleUtil;

public class BundleFhirDal implements FhirDal {

    protected FhirContext context;
    protected IBaseBundle bundle;
    protected IFhirPath fhirPath;

    public BundleFhirDal(FhirContext context, IBaseBundle bundle) {
        this.context = context;
        this.bundle = bundle;
        this.fhirPath = this.context.newFhirPath();
    }

    @Override
    @SuppressWarnings("unchecked")
    public IBaseResource read(IIdType id) {
        List<IBaseResource> resources = (List<IBaseResource>) BundleUtil.toListOfResourcesOfType(this.context,
                this.bundle,
                this.context.getResourceDefinition(id.getResourceType()).getImplementingClass());

        for (IBaseResource resource : resources) {
            if (resource.getIdElement().getIdPart().equals(id.getIdPart())) {
                return resource;
            }
        }

        return null;
    }

    @Override
    public void create(IBaseResource resource) {
        throw new NotImplementedException();
    }

    @Override
    public void update(IBaseResource resource) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(IIdType id) {
        throw new NotImplementedException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<IBaseResource> search(String resourceType) {
        return (Iterable<IBaseResource>) BundleUtil.toListOfResourcesOfType(this.context, this.bundle,
                this.context.getResourceDefinition(resourceType).getImplementingClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        List<IBaseResource> resources = (List<IBaseResource>) BundleUtil.toListOfResourcesOfType(this.context,
                this.bundle, this.context.getResourceDefinition(resourceType).getImplementingClass());

        List<IBaseResource> returnList = new ArrayList<>();
        for (IBaseResource resource : resources) {
            switch (this.context.getVersion().getVersion()) {
                case DSTU3:
                    var dstu3String = this.fhirPath.evaluateFirst(resource, "url",
                            org.hl7.fhir.dstu3.model.UriType.class);
                    if (dstu3String.isPresent() && dstu3String.get().getValue().equals(url)) {
                        returnList.add(resource);
                    }
                    break;

                case R4:
                    var r4String = this.fhirPath.evaluateFirst(resource, "url",
                            org.hl7.fhir.r4.model.UriType.class);
                    if (r4String.isPresent() && r4String.get().getValue().equals(url)) {
                        returnList.add(resource);
                    }
                    break;

                default:
                    throw new IllegalArgumentException(
                            String.format("Unsupported FHIR version %s", this.context.getVersion().getVersion()));
            }
        }

        return returnList;
    }
}
