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

    /***
     * Accepts a FHIR ParametersParameterComponent and creates an adapter to get the value.
     * @param fhirVersion The version of FHIR.
     * @param parameter   The FHIR ParametersParameterComponent of the specified version.  Using the inherited IBaseBackboneElement to avoid version specific types.
     * @return            The value of the parameter.
     */
    public static IBaseDatatype getValue(FhirVersionEnum fhirVersion, IBaseBackboneElement parameter) {
        if (parameter == null) {
            return null;
        }
        return ((IParametersParameterComponentAdapter) IAdapterFactory.createAdapterForBase(fhirVersion, parameter))
                .getValue();
    }

    /***
     * Accepts a FHIR ParametersParameterComponent that may contain a StringType or ReferenceType value and returns the value as a String.
     * @param fhirVersion The version of FHIR.
     * @param parameter   The FHIR ParametersParameterComponent of the specified version.  Using the inherited IBaseBackboneElement to avoid version specific types.
     * @return            The String value of the parameter.
     */
    public static String getStringOrReferenceValue(FhirVersionEnum fhirVersion, IBaseBackboneElement parameter) {
        var value = getValue(fhirVersion, parameter);
        if (value instanceof IBaseReference referenceValue) {
            return referenceValue.getReferenceElement().getValueAsString();
        } else if (value instanceof IPrimitiveType<?> primitiveValue) {
            return primitiveValue.getValueAsString();
        }
        return null;
    }

    /***
     * Accepts a FHIR ParametersParameterComponent and returns the value as a String if it is a Primitive type.
     * @param fhirVersion The version of FHIR.
     * @param parameter   The FHIR ParametersParameterComponent of the specified version.  Using the inherited IBaseBackboneElement to avoid version specific types.
     * @return            The String value of the parameter.
     */
    public static String getStringValue(FhirVersionEnum fhirVersion, IBaseBackboneElement parameter) {
        var value = getValue(fhirVersion, parameter);
        return getStringValue(value);
    }

    /***
     * Accepts a DataType and returns the value as a String if it is a Primitive type.
     * @param value The FHIR data type.
     * @return      The String value of the data type.
     */
    public static String getStringValue(IBaseDatatype value) {
        if (value instanceof IPrimitiveType<?> primitiveValue) {
            return primitiveValue.getValueAsString();
        }
        return null;
    }
}
