package org.opencds.cqf.cql.evaluator.fhir.dal;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeFhirDal implements FhirDal{
    private static final Logger logger = LoggerFactory.getLogger(CompositeFhirDal.class);

    protected BundleFhirDal bundleFhirDal;
    protected FhirDal[] fhirDals;

    public CompositeFhirDal(BundleFhirDal bundleFhirDal, FhirDal... fhirDals) {
        this.bundleFhirDal = bundleFhirDal;
        this.fhirDals = fhirDals;
    }

    @Override
    public IBaseResource read(IIdType id) {
        IBaseResource resource =  null;

        for(FhirDal fhirDal : fhirDals ) {
            resource = read(fhirDal, id);
            if(resource != null) {
                return resource;
            }
        }

        if(bundleFhirDal != null) {
            return bundleFhirDal.read(id);
        }

        return resource;
    }

    @Override
    public void create(IBaseResource resource) {

        if (fhirDals.length == 0) {
            throw new NotImplementedException();
        }

        boolean created = false;
        for (FhirDal fhirDal : fhirDals) {
            if (!created) {
                try {
                    fhirDal.create(resource);
                    created = true;
                    break;
                } catch (Exception e) {
                    logger.info(e.getMessage());
                    created = false;
                }
            }
        }
    }

    @Override
    public void update(IBaseResource resource) {
        if (fhirDals.length == 0) {
            throw new NotImplementedException();
        }

        boolean updated = false;
        for (FhirDal fhirDal : fhirDals) {
            if (!updated) {
                try {
                    fhirDal.update(resource);
                    updated = true;
                    break;
                } catch (Exception e) {
                    logger.info(e.getMessage());
                    updated = false;
                }
            }
        }
    }

    @Override
    public void delete(IIdType id) {
        if (fhirDals.length == 0) {
            throw new NotImplementedException();
        }

        boolean deleted = false;
        for (FhirDal fhirDal : fhirDals) {
            if (!deleted) {
                try {
                    fhirDal.delete(id);
                    deleted = true;
                    break;
                } catch (Exception e) {
                    logger.info(e.getMessage());
                    deleted = false;
                }
            }
        }
    }

    @Override
    public Iterable<IBaseResource> search(String resourceType) {
        Iterable<IBaseResource> returnResources = null;

        for (FhirDal fhirDal : fhirDals) {
            returnResources = concat(returnResources, search(fhirDal, resourceType));
        }

        if (bundleFhirDal != null) {
            returnResources = concat(returnResources, bundleFhirDal.search(resourceType));
        }

        return returnResources;
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        Iterable<IBaseResource> returnResources = null;

        for (FhirDal fhirDal : fhirDals) {
            returnResources = concat(returnResources, searchByUrl(fhirDal, resourceType, url));
        }

        if (bundleFhirDal != null) {
            returnResources = concat(returnResources,
                    bundleFhirDal.searchByUrl(resourceType, url));
        }
        return returnResources;
    }

    private IBaseResource read(FhirDal fhirDal, IIdType id) {
        IBaseResource resource = null;
        try {
            if (fhirDal != null) {
                resource = fhirDal.read(id);
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return resource;
    }

    private Iterable<IBaseResource> search(FhirDal fhirDal, String resourceType) {
        Iterable<IBaseResource> returnResources = null;

        try {
            if (fhirDal != null) {
                returnResources = fhirDal.search(resourceType);
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return returnResources;
    }

    private Iterable<IBaseResource> searchByUrl(FhirDal fhirDal, String resourceType, String url) {
        Iterable<IBaseResource> returnResources = null;

        try {
            if (fhirDal != null) {
                returnResources = fhirDal.searchByUrl(resourceType, url);
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return returnResources;
    }

    private Iterable<IBaseResource> concat(Iterable<IBaseResource> a, Iterable<IBaseResource> b){
        if( a == null) {
            return b;
        } else if(b == null) {
            return a;
        }
        return Iterables.concat(a, b);
    }
}
