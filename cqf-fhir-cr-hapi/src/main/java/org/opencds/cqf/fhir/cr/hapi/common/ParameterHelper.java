package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

/***
 * A utility class for parsing values from Parameters
 */
public class ParameterHelper {

    private ParameterHelper() {}

    public static IBaseDatatype getValue(FhirVersionEnum fhirVersion, IBaseBackboneElement parameter) {
        if (parameter == null) {
            return null;
        }
        return ((IParametersParameterComponentAdapter) IAdapterFactory.createAdapterForBase(fhirVersion, parameter))
                .getValue();
    }

    public static String getStringOrReferenceValue(FhirVersionEnum fhirVersion, IBaseBackboneElement parameter) {
        var value = getValue(fhirVersion, parameter);
        if (value instanceof IBaseReference referenceValue) {
            return referenceValue.getReferenceElement().getValueAsString();
        } else if (value instanceof IPrimitiveType<?> primitiveValue) {
            return primitiveValue.getValueAsString();
        }
        return null;
    }

    public static String getStringValue(FhirVersionEnum fhirVersion, IBaseBackboneElement parameter) {
        var value = getValue(fhirVersion, parameter);
        return getStringValue(value);
    }

    public static String getStringValue(IBaseDatatype value) {
        if (value instanceof IPrimitiveType<?> primitiveValue) {
            return primitiveValue.getValueAsString();
        }
        return null;
    }
}
