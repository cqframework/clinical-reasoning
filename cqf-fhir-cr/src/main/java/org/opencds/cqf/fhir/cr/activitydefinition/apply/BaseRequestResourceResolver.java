package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRequestResourceResolver {
    protected static final Logger logger = LoggerFactory.getLogger(BaseRequestResourceResolver.class);
    public static final String TARGET_STATUS_URL = "http://hl7.org/fhir/us/ecr/StructureDefinition/targetStatus";
    public static final String PRODUCT_ERROR_PREAMBLE = "Product does not map to ";
    public static final String DOSAGE_ERROR_PREAMBLE = "Dosage does not map to ";
    public static final String BODYSITE_ERROR_PREAMBLE = "BodySite does not map to ";
    public static final String CODE_ERROR_PREAMBLE = "Code does not map to ";
    public static final String QUANTITY_ERROR_PREAMBLE = "Quantity does not map to ";
    public static final String MISSING_CODE_PROPERTY = "Missing required code property";

    public abstract IBaseResource resolve(ICpgRequest request);
}
