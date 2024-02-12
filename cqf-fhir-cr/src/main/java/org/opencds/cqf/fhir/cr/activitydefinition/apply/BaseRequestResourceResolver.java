package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRequestResourceResolver {
    protected static final Logger logger = LoggerFactory.getLogger(BaseRequestResourceResolver.class);
    public static final String RESOLVE_MESSAGE = "Resolving {} to {}";
    public static final String TARGET_STATUS_URL = "http://hl7.org/fhir/us/ecr/StructureDefinition/targetStatus";
    public static final String MISSING_CODE_PROPERTY = "Missing required ActivityDefinition.code property for %s";
    public static final String MISSING_PRODUCT_PROPERTY = "Missing required ActivityDefinition.product property for %s";

    public abstract IBaseResource resolve(ICpgRequest request);
}
