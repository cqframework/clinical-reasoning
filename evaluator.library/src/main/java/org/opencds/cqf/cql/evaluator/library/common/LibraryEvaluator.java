package org.opencds.cqf.cql.evaluator.library.common;

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
import org.opencds.cqf.cql.evaluator.fhir.api.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.api.ParametersParameterComponentAdapter;

import ca.uhn.fhir.context.FhirContext;

import static org.opencds.cqf.cql.evaluator.fhir.common.AdapterFactory.parametersAdapterFor;
import static org.opencds.cqf.cql.evaluator.fhir.common.AdapterFactory.parametersParametersComponentAdapterFor;

public class LibraryEvaluator implements org.opencds.cqf.cql.evaluator.library.api.LibraryEvaluator {

    private CqlEvaluator cqlEvaluator;
    private FhirContext fhirContext;

    public LibraryEvaluator(FhirContext fhirContext, CqlEvaluator cqlEvaluator) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.cqlEvaluator = Objects.requireNonNull(cqlEvaluator, "cqlEvaluator can not be null");
    }

    protected IBaseParameters toParameters(EvaluationResult result) {
        IBaseParameters params = null;
        try {
            params = (IBaseParameters)this.fhirContext.getResourceDefinition("Parameters").getImplementingClass().getConstructor().newInstance();
            ParametersAdapter pa = parametersAdapterFor(params);

            for (Map.Entry<String, Object> entry : result.expressionResults.entrySet())
            {
                IBaseBackboneElement ppc = pa.addParameter(params);

                ParametersParameterComponentAdapter ppca = parametersParametersComponentAdapterFor(ppc);

                ppca.setName(ppc, entry.getKey());

                Object value = entry.getValue();

                if (value instanceof IBaseResource) {
                    ppca.setResource(ppc, (IBaseResource)value);
                    continue;
                }

                if (value instanceof IBaseDatatype) {
                    ppca.setValue(ppc, (IBaseDatatype)value);
                    continue;
                }

                if (value instanceof CqlType) {
                    ppca.setValue(ppc, (IBaseDatatype)this.fhirContext.getResourceDefinition("StringType")
                        .getImplementingClass()
                        .getConstructor(String.class)
                        .newInstance(value.toString()));
                    continue;
                }
            }
        }
        catch (Exception e) {

        }

        return params;
    }

    protected Map<String, Object> toParametersMap(VersionedIdentifier libraryIdentifier, IBaseParameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();

        ParametersAdapter parametersAdapter = parametersAdapterFor(parameters);
        if (parametersAdapter.getParameter(parameters) == null) {
            return parameterMap;
        }

        for (IBaseBackboneElement ppc : parametersAdapter.getParameter(parameters)) {
            ParametersParameterComponentAdapter ppca = parametersParametersComponentAdapterFor(ppc);
            String name = ppca.getName(ppc);
            if (ppca.hasResource(ppc)) {
                parameterMap.put(name, ppca.getResource(ppc));
            }
            else if (ppca.hasValue(ppc)) {
                parameterMap.put(name, ppca.getValue(ppc).toString());
            }
        }

        return parameterMap;
    }

    @Override
    public IBaseParameters evaluate(
        VersionedIdentifier libraryIdentifier,
        Pair<String, Object> contextParameter,
        IBaseParameters parameters,
        Set<String> expressions)
        {
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

            EvaluationResult result = this.cqlEvaluator.evaluate(libraryIdentifier, expressions, contextParameter, evaluationParameters);
            return toParameters(result);
        }
}