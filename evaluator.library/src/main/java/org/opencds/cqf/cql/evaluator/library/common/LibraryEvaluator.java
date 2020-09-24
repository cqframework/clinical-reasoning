package org.opencds.cqf.cql.evaluator.library.common;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.runtime.CqlType;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

class LibraryEvaluator {

    Logger logger = LoggerFactory.getLogger(LibraryEvaluator.class);

    private CqlEvaluator cqlEvaluator;
    private FhirContext fhirContext;
    private AdapterFactory adapterFactory;

    LibraryEvaluator(FhirContext fhirContext, AdapterFactory adapterFactory, CqlEvaluator cqlEvaluator) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.cqlEvaluator = Objects.requireNonNull(cqlEvaluator, "cqlEvaluator can not be null");
        this.adapterFactory = Objects.requireNonNull(adapterFactory, "adapterFactory can not be null");
    }

    protected IBaseParameters toParameters(EvaluationResult result) {
        IBaseParameters params = null;
        try {
            params = (IBaseParameters) this.fhirContext.getResourceDefinition("Parameters").getImplementingClass()
                    .getConstructor().newInstance();
            ParametersAdapter pa = this.adapterFactory.createParameters(params);

            for (Map.Entry<String, Object> entry : result.expressionResults.entrySet()) {
                try {

                    IBaseBackboneElement ppc = pa.addParameter();

                    ParametersParameterComponentAdapter parametersComponent = this.adapterFactory
                            .createParametersParameters(ppc);
                    parametersComponent.setName(entry.getKey());

                    Object value = entry.getValue();

                    if (value instanceof Iterable) {
                        for (Object v : (Iterable<?>) value) {
                            addValuePart(parametersComponent, v);
                        }
                    } else {
                        addValuePart(parametersComponent, value);
                    }
                } catch (Exception e) {
                    logger.error(String.format("Error trying to create parameter for evaluation of %s", entry.getKey()),
                            e);
                }
            }
        } catch (Exception e) {
            logger.error("Error trying to create Parameters resource", e);
        }

        return params;
    }

    protected void addValuePart(ParametersParameterComponentAdapter ppca, Object value)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ParametersParameterComponentAdapter valueComponent = this.adapterFactory
                .createParametersParameters(ppca.addPart());

        valueComponent.setName("value");
        if (value == null) {
            // The name "value" without a value signifies null
        }
        if (value instanceof IBaseResource) {
            valueComponent.setResource((IBaseResource) value);
        } else if (value instanceof IBaseDatatype) {
            valueComponent.setValue((IBaseDatatype) value);
        } else if (value instanceof CqlType) {
            valueComponent.setValue((IBaseDatatype) this.fhirContext.getElementDefinition("String")
                    .getImplementingClass().getConstructor(String.class).newInstance(value.toString()));
        } else if (value instanceof Boolean) {
            valueComponent.setValue((IBaseDatatype) this.fhirContext.getElementDefinition("Boolean")
                    .getImplementingClass().getConstructor(Boolean.class).newInstance(value));
        } else {
            logger.info(String.format("Unknown type encountered %s", value.getClass().getSimpleName()));
            valueComponent.setValue((IBaseDatatype) this.fhirContext.getElementDefinition("String")
                    .getImplementingClass().getConstructor(String.class).newInstance(value.toString()));
        }
    }

    protected Map<String, Object> toParametersMap(VersionedIdentifier libraryIdentifier, IBaseParameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();

        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        if (parametersAdapter.getParameter() == null) {
            return parameterMap;
        }

        for (IBaseBackboneElement ppc : parametersAdapter.getParameter()) {
            ParametersParameterComponentAdapter parametersComponent = this.adapterFactory
                    .createParametersParameters(ppc);
            String name = parametersComponent.getName();
            if (parametersComponent.hasResource()) {
                parameterMap.put(name, parametersComponent.getResource());
            }
            // } else if (parametersComponent.hasValue()) {
            //     parameterMap.put(name, this.parameterParser.parseParameter(this.libraryLoader, libraryIdentifier, name,
            //             parametersComponent.getValue().toString()));
            // }
        }

        return parameterMap;
    }

    public IBaseParameters evaluate(VersionedIdentifier libraryIdentifier, Pair<String, Object> contextParameter,
            IBaseParameters parameters, Set<String> expressions) {
        Map<String, Object> evaluationParameters = null;
        
        if (parameters != null) {
            if (!parameters.fhirType().equals("Parameters")) {
                throw new IllegalArgumentException("parameters is not a FHIR Parameters resource");
            }

            if (!parameters.getStructureFhirVersionEnum().equals(this.fhirContext.getVersion().getVersion())) {
                throw new IllegalArgumentException("the FHIR versions of parameters and fhirContext do not match");
            }

            evaluationParameters = this.toParametersMap(libraryIdentifier, parameters);
        }

        EvaluationResult result = this.cqlEvaluator.evaluate(libraryIdentifier, expressions, contextParameter,
                evaluationParameters);
        return toParameters(result);
    }
}