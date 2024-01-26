package org.opencds.cqf.fhir.utility.r5;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.Base64BinaryType;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.DateType;
import org.hl7.fhir.r5.model.DecimalType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.InstantType;
import org.hl7.fhir.r5.model.Integer64Type;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.MarkdownType;
import org.hl7.fhir.r5.model.OidType;
import org.hl7.fhir.r5.model.PositiveIntType;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.TimeType;
import org.hl7.fhir.r5.model.UnsignedIntType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.UrlType;
import org.hl7.fhir.r5.model.UuidType;
import org.opencds.cqf.fhir.utility.Ids;

/**
 * Utility class for R5 parameters
 */
public class Parameters {

    private Parameters() {}

    /**
     * Method to create parameters for r5
     *
     * @param id IdType of measure
     * @param parts Parameter r5 components
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters parameters(
            IdType id, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        checkNotNull(id);
        org.hl7.fhir.r5.model.Parameters p = parameters(parts);
        p.setId(id);
        return p;
    }

    /**
     * Method to create a parameter for r5
     *
     * @param idPart String representing the Id of measure
     * @param parts Parameter r5 components
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters parameters(
            String idPart, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        checkNotNull(idPart);
        return parameters((IdType) Ids.newId(org.hl7.fhir.r5.model.Parameters.class, idPart), parts);
    }

    /**
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters parameters(
            org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        org.hl7.fhir.r5.model.Parameters p = new org.hl7.fhir.r5.model.Parameters();
        for (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent c : parts) {
            p.addParameter(c);
        }
        return p;
    }

    /**
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent part(
            String name, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        checkNotNull(name);
        org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent c =
                new org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent();
        c.setName(name);
        for (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent p : parts) {
            c.addPart(p);
        }
        return c;
    }

    /**
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @param value String representing value of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent part(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        checkNotNull(name);
        checkNotNull(value);
        org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent c = part(name, parts);
        c.setValue(new StringType(value));
        return c;
    }

    /**
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @param value Datatype representing value of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent part(
            String name,
            org.hl7.fhir.r5.model.DataType value,
            org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        checkNotNull(name);
        checkNotNull(value);
        org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent c = part(name, parts);
        c.setValue(value);
        return c;
    }

    /**
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent part(
            String name,
            org.hl7.fhir.r5.model.Resource resource,
            org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        checkNotNull(name);
        checkNotNull(resource);
        org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent c = part(name, parts);
        c.setResource(resource);
        return c;
    }

    /**
     * Method to get a parameter parts for r5
     *
     * @param parameters Parameter r5 components
     * @param name String representing parameter name
     * @return parameters
     */
    public static List<org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent> getPartsByName(
            org.hl7.fhir.r5.model.Parameters parameters, String name) {
        checkNotNull(parameters);
        checkNotNull(name);
        return parameters.getParameter().stream()
                .filter(x -> name.equals(x.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Method to get base64 parameter part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent base64BinaryPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new Base64BinaryType(value), parts);
    }

    /**
     * Method to get boolean parameter part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent booleanPart(
            String name, boolean value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new BooleanType(value), parts);
    }

    /**
     * Method to get canonical parameter part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent canonicalPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new CanonicalType(value), parts);
    }

    /**
     * Method to get code part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent codePart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new CodeType(value), parts);
    }

    /**
     * Method to get datepart for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent datePart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new DateType(value), parts);
    }

    /**
     * Method to get dateTime part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent dateTimePart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new DateTimeType(value), parts);
    }

    /**
     * Method to get decimal part for r5
     *
     * @param name Parameter part name
     * @param value double representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent decimalPart(
            String name, double value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new DecimalType(value), parts);
    }

    /**
     * Method to get decimal idpart for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent idPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new IdType(value), parts);
    }

    /**
     * Method to get instant part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent instantPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new InstantType(value), parts);
    }

    /**
     * Method to get integer part for r5
     *
     * @param name Parameter part name
     * @param value int representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent integerPart(
            String name, int value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new IntegerType(value), parts);
    }

    /**
     * Method to get integer part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent integer64Part(
            String name, long value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new Integer64Type(value), parts);
    }

    /**
     * Method to get markdown part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent markdownPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new MarkdownType(value), parts);
    }

    /**
     * Method to get OID part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent oidPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new OidType(value), parts);
    }

    /**
     * Method to get positive int part for r5
     *
     * @param name Parameter part name
     * @param value Int representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent positiveIntPart(
            String name, int value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new PositiveIntType(value), parts);
    }

    /**
     * Method to get String part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent stringPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new StringType(value), parts);
    }

    /**
     * Method to get time part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent timePart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new TimeType(value), parts);
    }

    /**
     * Method to get unsigned int part for r5
     *
     * @param name Parameter part name
     * @param value Int representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent unsignedIntPart(
            String name, int value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new UnsignedIntType(value), parts);
    }

    /**
     * Method to get unsigned uri part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent uriPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new UriType(value), parts);
    }

    /**
     * Method to get unsigned url part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent urlPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new UrlType(value), parts);
    }

    /**
     * Method to get unsigned uuid part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    public static org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent uuidPart(
            String name, String value, org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent... parts) {
        return part(name, new UuidType(value), parts);
    }
}
