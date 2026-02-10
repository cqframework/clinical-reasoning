package org.opencds.cqf.fhir.cr.hapi.common;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Ids;

public class IdHelper {

    private IdHelper() {}

    public static IIdType getIdType(FhirVersionEnum fhirVersion, String resourceType, IBaseDatatype id) {
        return getIdType(fhirVersion, resourceType, getStringValue(id));
    }

    public static IIdType getIdType(FhirVersionEnum fhirVersion, String resourceType, String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return Ids.newId(fhirVersion, resourceType, id.replace(resourceType + "/", ""));
    }
}
