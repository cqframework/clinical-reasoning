package org.opencds.cqf.cql.evaluator.fhir.dal;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeFhirDal implements FhirDal{
    private static final Logger logger = LoggerFactory.getLogger(CompositeFhirDal.class);

    protected FhirDal jpaFhirDal;
    protected FhirDal endPointDal;
    protected BundleFhirDal bundleFhirDal;

    public CompositeFhirDal(FhirDal jpaFhirDal, FhirDal endPointDal, BundleFhirDal bundleFhirDal) {
        this.jpaFhirDal = jpaFhirDal;
        this.endPointDal = endPointDal;
        this.bundleFhirDal = bundleFhirDal;
    }

    @Override
    public IBaseResource read(IIdType id) {
        IBaseResource resource;

        resource = read(jpaFhirDal, id);
        if (resource == null) {
            resource = read(endPointDal, id);
        }

        if(resource == null && bundleFhirDal != null) {
            resource = bundleFhirDal.read(id);
        }

        return resource;
    }

    @Override
    public void create(IBaseResource resource) {
        if (jpaFhirDal == null && endPointDal == null) {
            throw new NotImplementedException();
        } else if (endPointDal == null) {
            try {
                jpaFhirDal.create(resource);
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        } else if (jpaFhirDal == null) {
            endPointDal.create(resource);
        }
    }

    @Override
    public void update(IBaseResource resource) {
        if (jpaFhirDal == null && endPointDal == null) {
            throw new NotImplementedException();
        } else if (endPointDal == null) {
            try {
                jpaFhirDal.update(resource);
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        } else if (jpaFhirDal == null) {
            endPointDal.update(resource);
        }
    }

    @Override
    public void delete(IIdType id) {
        if (jpaFhirDal == null && endPointDal == null) {
            throw new NotImplementedException();
        } else if (endPointDal == null) {
            jpaFhirDal.delete(id);
        } else if (jpaFhirDal == null) {
            endPointDal.delete(id);
        }
    }

    @Override
    public Iterable<IBaseResource> search(String resourceType) {
        Iterable<IBaseResource> returnResources;

        returnResources = search(jpaFhirDal, resourceType);
        returnResources = concat(returnResources, search(endPointDal, resourceType));

        if (bundleFhirDal != null) {
            returnResources = concat(returnResources, bundleFhirDal.search(resourceType));
        }

        return returnResources;
    }

    @Override
    public Iterable<IBaseResource> searchByUrl(String resourceType, String url) {
        Iterable<IBaseResource> returnResources;

        returnResources = searchByUrl(jpaFhirDal, resourceType, url);
        returnResources = concat(returnResources, searchByUrl(endPointDal, resourceType, url));

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
