package org.opencds.cqf.fhir.utility.operation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.util.ParametersUtil;
import jakarta.annotation.Nonnull;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.Resources;

interface ParameterBinder {
    enum Type {
        ID,
        OPERATION,
        EXTRA
    }

    // Extract the value from the parameters resource that matches the external name of the parameter.
    // And removes the value from the parameters resource. This is to ensure that all parameters get consumed
    Object bind(IBaseParameters parameters);

    @Nonnull
    Type type();

    @Nonnull
    String name();

    @Nonnull
    Parameter parameter();

    // Ensures that the parameter annotations are valid for the operation
    // Requirements specific to a given parameter are checked in the ParameterBinder
    // This method checks for cross-parameter requirements
    static List<ParameterBinder> validate(List<ParameterBinder> parameterBinders) {
        var idParamCount =
                parameterBinders.stream().filter(x -> x.type() == Type.ID).count();
        if (idParamCount > 1) {
            throw new IllegalArgumentException("Method cannot have more than one @IdParam");
        }

        if (idParamCount > 0 && parameterBinders.get(0).type() != Type.ID) {
            throw new IllegalArgumentException("If @IdParam is present, it must be the first parameter");
        }

        var unboundParamCount =
                parameterBinders.stream().filter(x -> x.type() == Type.EXTRA).count();
        if (unboundParamCount > 1) {
            throw new IllegalArgumentException("Method cannot have more than one @UnboundParam");
        }

        if (unboundParamCount > 0
                && parameterBinders.get(parameterBinders.size() - 1).type() != Type.EXTRA) {
            throw new IllegalArgumentException("If @UnboundParam is present, it must be the last parameter");
        }

        return parameterBinders;
    }

    static ParameterBinder from(Parameter parameter) {
        requireNonNull(parameter, "parameter can not be null");

        var idParam = parameter.getAnnotation(IdParam.class);
        var operationParam = parameter.getAnnotation(OperationParam.class);
        var extraParam = parameter.getAnnotation(ExtraParams.class);

        ensureExactlyOneOf(idParam, operationParam, extraParam);

        if (idParam != null) {
            return new IdParameterBinder(parameter);
        } else if (operationParam != null) {
            return new OperationParameterBinder(parameter, operationParam);
        } else {
            return new ExtraParamBinder(parameter);
        }
    }

    static void ensureExactlyOneOf(IdParam idParam, OperationParam operationParam, ExtraParams extraParams) {

        var count = Arrays.asList(idParam, operationParam, extraParams).stream()
                .filter(Objects::nonNull)
                .count();

        if (count == 0) {
            throw new IllegalArgumentException(
                    "Method Parameter must be annotated with @IdParam, @OperationParam, or @ExtraParams");
        } else if (count > 1) {
            throw new IllegalArgumentException(
                    "Method Parameter can only be annotated with one of @IdParam, @OperationParam, or @ExtraParams");
        }
    }

    static class IdParameterBinder implements ParameterBinder {
        private final Parameter parameter;

        public IdParameterBinder(Parameter parameter) {
            this.parameter = requireNonNull(parameter, "parameter can not be null");
            checkArgument(
                    IIdType.class.isAssignableFrom(parameter.getType()),
                    "Parameter annotated with @IdParam must be of type IIdType");
        }

        @Override
        public Type type() {
            return Type.ID;
        }

        @Override
        public String name() {
            return "_id";
        }

        @Override
        public Object bind(IBaseParameters parameters) {
            throw new UnsupportedOperationException("bind is not supported for @IdParam");
        }

        @Override
        public Parameter parameter() {
            return parameter;
        }
    }

    static class OperationParameterBinder implements ParameterBinder {

        private enum ParameterClass {
            RESOURCE,
            VALUE,
            LIST
        }

        private final Parameter parameter;
        private final OperationParam operationParam;
        private final ParameterClass parameterClass;

        public OperationParameterBinder(Parameter parameter, OperationParam operationParam) {
            this.parameter = requireNonNull(parameter, "parameter can not be null");
            this.operationParam = requireNonNull(operationParam, "operationParam can not be null");
            requireNonNull(operationParam.name(), "@OperationParam must have a name defined");
            this.parameterClass = determineParameterClass(parameter);
        }

        private static ParameterClass determineParameterClass(Parameter parameter) {
            var type = parameter.getType();
            if (IBaseResource.class.isAssignableFrom(type)) {
                return ParameterClass.RESOURCE;
            } else if (IBaseDatatype.class.isAssignableFrom(type)) {
                return ParameterClass.VALUE;
            } else if (List.class.isAssignableFrom(type)) {
                return ParameterClass.LIST;
            } else {
                throw new IllegalArgumentException("Parameter annotated with @Operation must be a FHIR type or a List");
            }
        }

        @Override
        public Type type() {
            return Type.OPERATION;
        }

        @Override
        public String name() {
            return operationParam.name();
        }

        @Override
        public Object bind(IBaseParameters parameters) {
            if (parameters == null && operationParam.min() <= 0) {
                return null;
            } else if (parameters == null) {
                throw new IllegalArgumentException("Parameter " + this.name() + " is required but was not provided");
            }

            var context = parameters.getStructureFhirVersionEnum().newContextCached();
            var terser = context.newTerser();
            var params = ParametersUtil.getNamedParameters(context, parameters, this.name());
            Parameters.removeParameter(parameters, this.name());

            if (params.isEmpty() && operationParam.min() > 0) {
                throw new IllegalArgumentException("Parameter " + this.name() + " is required but was not provided");
            }

            if (params.size() > 1) {
                throw new IllegalArgumentException("Parameter " + this.name()
                        + " has more than one value. Use parameter parts for multiple values");
            }

            // Not required and not present so we can return null
            if (params.isEmpty()) {
                return null;
            }

            // Here, we need to get either the part, the resource, or the value[x] from the parameter
            // For list-valued parameters, we need to the value/resource from each part

            // First, let's check for parts. For lists this is correct.
            // For single-valued parameters, this is incorrect
            var parts = terser.getValues(params.get(0), "part");
            if (parameterClass != ParameterClass.LIST && !parts.isEmpty()) {
                throw new IllegalArgumentException(
                        "Parameter " + this.name() + " is not the expected type " + parameter.getType());
            }

            if (parameterClass == ParameterClass.LIST) {
                var size = parts.size();
                if (operationParam.min() > size) {
                    throw new IllegalArgumentException("Parameter " + this.name()
                            + " has fewer values than the minimum of " + operationParam.min());
                }

                if (operationParam.max() > 0 && size > operationParam.max()) {
                    throw new IllegalArgumentException("Parameter " + this.name()
                            + " has more values than the maximum of " + operationParam.max());
                }
            }

            if (parameterClass == ParameterClass.LIST) {
                var values = new ArrayList<IBase>();
                for (var part : parts) {
                    var r = terser.getSingleValueOrNull(part, "resource", IBaseResource.class);
                    var v = terser.getSingleValueOrNull(part, "value[x]", IBase.class);

                    if (v != null) {
                        values.add(v);
                    } else if (r != null) {
                        values.add(r);
                    } else {
                        throw new IllegalArgumentException(
                                "Parameter " + this.name() + " has an empty part. Expected a resource or value[x]");
                    }
                }

                return values;
            }

            var param = params.get(0);
            IBaseResource valueResource = terser.getSingleValueOrNull(param, "resource", IBaseResource.class);
            IBase value = terser.getSingleValueOrNull(param, "value[x]", IBase.class);
            if (valueResource != null && value != null) {
                throw new IllegalArgumentException(
                        "Parameter " + this.name() + " has both a resource and a value. Only one is allowed");
            }

            if (parameterClass == ParameterClass.RESOURCE) {
                if (value != null) {
                    throw new IllegalArgumentException(
                            "Parameter " + this.name() + " is not the expected type " + parameter.getType());
                }

                if (valueResource == null && operationParam.min() > 0) {
                    throw new IllegalArgumentException(
                            "Parameter " + this.name() + " is required but was not provided");
                }
            }

            if (parameterClass == ParameterClass.VALUE) {
                if (valueResource != null) {
                    throw new IllegalArgumentException(
                            "Parameter " + this.name() + " is not the expected type " + parameter.getType());
                }

                if (value == null && operationParam.min() > 0) {
                    throw new IllegalArgumentException(
                            "Parameter " + this.name() + " is required but was not provided");
                }
            }

            var returnValue = valueResource != null ? valueResource : value;
            if (returnValue == null) {
                return null;
            }

            try {
                return this.parameter.getType().cast(returnValue);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(
                        "Parameter value '" + this.name() + "'' is not of the expected type " + parameter.getType());
            }
        }

        @Override
        public Parameter parameter() {
            return parameter;
        }
    }

    static class ExtraParamBinder implements ParameterBinder {
        private final Parameter parameter;

        public ExtraParamBinder(Parameter parameter) {
            this.parameter = requireNonNull(parameter, "parameter can not be null");
            checkArgument(
                    IBaseParameters.class.isAssignableFrom(parameter.getType()),
                    "Parameter annotated with @UnboundParam must be of type IBaseParameters");
        }

        @Override
        public Type type() {
            return Type.EXTRA;
        }

        @Override
        public String name() {
            return "<unbound>";
        }

        @Override
        public Object bind(IBaseParameters parameters) {
            if (parameters == null) {
                return null;
            }

            var value = Resources.clone(parameters);

            // Remove all values from the parameters resource

            return value;
        }

        @Override
        public Parameter parameter() {
            return this.parameter;
        }
    }
}
