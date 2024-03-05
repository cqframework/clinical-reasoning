package org.opencds.cqf.fhir.utility.operation;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OperationParam;

class ParameterBinder {
    
    private final Parameter parameter;
    private final Optional<IdParam> idParam;
    private final Optional<OperationParam> operationParam;
    private final Optional<UnboundParam> unboundParam;
    ParameterBinder(Parameter parameter) {
        this.parameter = parameter;
        this.idParam = Optional.ofNullable(parameter.getAnnotation(IdParam.class));
        this.operationParam = Optional.ofNullable(parameter.getAnnotation(OperationParam.class));
        this.unboundParam = Optional.ofNullable(parameter.getAnnotation(UnboundParam.class));
        if (idParam.isEmpty() && operationParam.isEmpty() && unboundParam.isEmpty()) {
            throw new IllegalArgumentException("Parameter must be annotated with @IdParam, @OperationParam, or @UnboundParam");
        }
    }

    boolean isIdParam() {
        return idParam.isPresent();
    }

    Optional<IdParam> idParam() {
        return idParam;
    }

    boolean isUnboundParam() {
        return unboundParam.isPresent();
    }

    Optional<UnboundParam> unboundParam() {
        return unboundParam;
    }

    boolean isOperationParam() {
        return operationParam.isPresent();
    }

    Optional<OperationParam> operationParam() {
        return operationParam;
    }

    Parameter parameter() {
        return parameter;
    }

    String internalName(){
        return parameter.getName();
    }

    String externalName() {
        if (isIdParam()) {
            return "_id";
        }
        else if (isOperationParam()) {
            var name = operationParam.get().name();
            return name == null || name.isEmpty() ? internalName() : name;
        }
        else {
            return "<unbound>";
        }
    }

    public IBase value(IBaseParameters parameters) {

        // Here, we need to go grab the the value from the parameters that matches the
        // external name of the parameter. Also need to do type validation here.
        // And handle collection types like list.

        // Whatever values we take from the parameters, we need to remove them from the parameters
        // resource. If a Parameter is required, and it's not found, we need to throw an error.
        return null;
    }
}
