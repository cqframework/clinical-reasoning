package org.opencds.cqf.fhir.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * A utility class for parameter creation and functions in clinical reasoning
 */
public class Parameters {

    private Parameters() {}

    private static BaseRuntimeChildDefinition getParameterChild(FhirContext fhirContext) {
        return fhirContext.getResourceDefinition("Parameters").getChildByName("parameter");
    }

    private static BaseRuntimeElementDefinition<?> getParameterElement(FhirContext fhirContext) {
        return getParameterChild(fhirContext).getChildByName("parameter");
    }

    private static BaseRuntimeChildDefinition.IMutator getValueMutator(FhirContext fhirContext) {
        return getParameterElement(fhirContext).getChildByName("value[x]").getMutator();
    }

    private static void validateNameAndValue(String name, Object value) {
        checkNotNull(name);
        checkNotNull(value);
    }

    /**
     * Creates the appropriate parameters for a given FhirContext, IIDType, IBase Parts
     *
     * @param id an IIdType type
     * @param fhirContext the FhirContext for fhir API
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameters
     */
    public static IBaseParameters newParameters(FhirContext fhirContext, IIdType id, IBase... parts) {
        checkNotNull(id);
        IBaseParameters newParameters = ParametersUtil.newInstance(fhirContext);
        newParameters.setId(id);
        BaseRuntimeChildDefinition.IMutator mutator =
                getParameterChild(fhirContext).getMutator();
        for (IBase part : parts) {
            mutator.addValue(newParameters, part);
        }
        return newParameters;
    }

    /**
     * Creates the appropriate parameters for a given FhirContext, ID String, IBase Parts
     *
     * @param id String representation of the ID to generate
     * @param fhirContext the FhirContext for fhir API
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameters
     */
    public static IBaseParameters newParameters(FhirContext fhirContext, String id, IBase... parts) {
        checkNotNull(id);
        IIdType newId = (IIdType)
                Objects.requireNonNull(fhirContext.getElementDefinition("id")).newInstance();
        newId.setValue(id);
        return newParameters(fhirContext, newId, parts);
    }

    /**
     * Creates the appropriate parameters for a given FhirContext and IBase Parts
     *
     * @param fhirContext the FhirContext for fhir API
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameters
     */
    public static IBaseParameters newParameters(FhirContext fhirContext, IBase... parts) {
        IBaseParameters newParameters = ParametersUtil.newInstance(fhirContext);
        BaseRuntimeChildDefinition.IMutator mutator =
                getParameterChild(fhirContext).getMutator();
        for (IBase part : parts) {
            mutator.addValue(newParameters, part);
        }
        return newParameters;
    }

    /**
     * Creates new IBase parts given FhirContext, part name to get, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir API
     * @param name String representation of parts to add to parameters
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    public static IBase newPart(FhirContext fhirContext, String name, IBase... parts) {
        checkNotNull(name);
        BaseRuntimeChildDefinition.IMutator nameMutator =
                getParameterElement(fhirContext).getChildByName("name").getMutator();
        BaseRuntimeChildDefinition.IMutator partMutator =
                getParameterElement(fhirContext).getChildByName("part").getMutator();
        IBase parameterBase = getParameterElement(fhirContext).newInstance();
        IBase theName = Objects.requireNonNull(fhirContext.getElementDefinition("string"))
                .newInstance(name);
        nameMutator.setValue(parameterBase, theName);
        for (IBase part : parts) {
            partMutator.addValue(parameterBase, part);
        }
        return parameterBase;
    }

    /**
     * Creates new IBase parts given FhirContext, fhir element type, part name to get, Object
     * instance, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir AP
     * @param type Element definition type I
     * @param name String representation of parts to add to parameters
     * @param value Object instance
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    public static <T extends IBaseDatatype> IBase newPart(
            FhirContext fhirContext, Class<T> type, String name, Object value, IBase... parts) {
        validateNameAndValue(name, value);
        IBase newPpc = newPart(fhirContext, name, parts);
        IBase typeValue =
                Objects.requireNonNull(fhirContext.getElementDefinition(type)).newInstance(value);
        getValueMutator(fhirContext).setValue(newPpc, typeValue);
        return newPpc;
    }

    /**
     * Creates new IBase parts given FhirContext, fhir element type, part name to get, Object
     * instance, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir AP
     * @param typeName String representation of FhirElement definition type I
     * @param name String representation of parts to add to parameters
     * @param value Object instance
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    public static IBase newPart(FhirContext fhirContext, String typeName, String name, Object value, IBase... parts) {
        validateNameAndValue(name, value);
        IBase newPpc = newPart(fhirContext, name, parts);
        IBase typeValue = Objects.requireNonNull(fhirContext.getElementDefinition(typeName))
                .newInstance(value.toString());
        getValueMutator(fhirContext).setValue(newPpc, typeValue);
        return newPpc;
    }

    /**
     * Creates new IBase parts given FhirContext, fhir element type, part name to get, Object
     * instance, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parts to add to parameters
     * @param value IBaseResource value
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    public static IBase newPart(FhirContext fhirContext, String name, IBaseResource value, IBase... parts) {
        validateNameAndValue(name, value);
        IBase newPpc = newPart(fhirContext, name, parts);
        getParameterElement(fhirContext).getChildByName("resource").getMutator().setValue(newPpc, value);
        return newPpc;
    }

    /**
     * method get string named parameter using FhirContext, Parameters, and name
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param parameters IBaseResource values
     * @return parameter string name
     */
    public static Optional<String> getSingularStringPart(
            FhirContext fhirContext, IBaseResource parameters, String name) {
        checkNotNull(parameters);
        checkNotNull(name);
        return ParametersUtil.getNamedParameterValueAsString(fhirContext, (IBaseParameters) parameters, name);
    }

    /**
     * method get string named part from parameters using FhirContext, Parameters, and name
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param parameters IBaseResource values
     * @return parameter string name
     */
    public static List<IBase> getPartsByName(FhirContext fhirContext, IBaseResource parameters, String name) {
        checkNotNull(parameters);
        checkNotNull(name);
        return ParametersUtil.getNamedParameters(fhirContext, parameters, name);
    }

    /**
     * method create base64binary part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value Name of part value
     * @param parts IBase type parameter parts
     * @return new base64 binary part
     */
    public static IBase newBase64BinaryPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "base64binary", name, value, parts);
    }

    /**
     * method create boolean part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new boolean part
     */
    public static IBase newBooleanPart(FhirContext fhirContext, String name, boolean value, IBase... parts) {
        return newPart(fhirContext, "boolean", name, value, parts);
    }

    /**
     * method create canonical part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new canonical part
     */
    public static IBase newCanonicalPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "canonical", name, value, parts);
    }

    /**
     * method create code part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new code part
     */
    public static IBase newCodePart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "code", name, value, parts);
    }

    /**
     * method create date part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new date part
     */
    public static IBase newDatePart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "date", name, value, parts);
    }

    /**
     * method create datetime part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new datetime part
     */
    public static IBase newDateTimePart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "datetime", name, value, parts);
    }

    /**
     * method create decimal part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new decimal part
     */
    public static IBase newDecimalPart(FhirContext fhirContext, String name, double value, IBase... parts) {
        return newPart(fhirContext, "decimal", name, value, parts);
    }

    /**
     * method create ID part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new ID part
     */
    public static IBase newIdPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "id", name, value, parts);
    }

    /**
     * method create instant part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new instant part
     */
    public static IBase newInstantPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "instant", name, value, parts);
    }

    /**
     * method create integer part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new integer part
     */
    public static IBase newIntegerPart(FhirContext fhirContext, String name, int value, IBase... parts) {
        return newPart(fhirContext, "integer", name, value, parts);
    }

    /**
     * method create integer base 64 part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new base64 integer part
     */
    public static IBase newInteger64Part(FhirContext fhirContext, String name, long value, IBase... parts) {
        return newPart(fhirContext, "integer64", name, value, parts);
    }

    /**
     * method create markdown part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new markdown part
     */
    public static IBase newMarkdownPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "markdown", name, value, parts);
    }

    /**
     * method create OID part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new OID part
     */
    public static IBase newOidPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "oid", name, value, parts);
    }

    /**
     * method create positive int part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new positive int part
     */
    public static IBase newPositiveIntPart(FhirContext fhirContext, String name, int value, IBase... parts) {
        return newPart(fhirContext, "positiveint", name, value, parts);
    }

    /**
     * method create string part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new string part
     */
    public static IBase newStringPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "string", name, value, parts);
    }

    /**
     * method create time part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new time part
     */
    public static IBase newTimePart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "time", name, value, parts);
    }

    /**
     * method create unsigned int part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new unsigned int part
     */
    public static IBase newUnsignedIntPart(FhirContext fhirContext, String name, int value, IBase... parts) {
        return newPart(fhirContext, "unsignedint", name, value, parts);
    }

    /**
     * method create uri part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new uri part
     */
    public static IBase newUriPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "uri", name, value, parts);
    }

    /**
     * method create url part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new url part
     */
    public static IBase newUrlPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "url", name, value, parts);
    }

    /**
     * method create uuid part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new uuid part
     */
    public static IBase newUuidPart(FhirContext fhirContext, String name, String value, IBase... parts) {
        return newPart(fhirContext, "uuid", name, value, parts);
    }
}
