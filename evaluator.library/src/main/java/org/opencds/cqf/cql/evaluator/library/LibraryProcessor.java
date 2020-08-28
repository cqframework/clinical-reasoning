package org.opencds.cqf.cql.evaluator.library;

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
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.runtime.CqlType;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.api.ParameterParser;
import org.opencds.cqf.cql.evaluator.fhir.api.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.api.ParametersParameterComponentAdapter;

import ca.uhn.fhir.context.FhirContext;

import static org.opencds.cqf.cql.evaluator.fhir.AdapterFactory.parametersAdapterFor;
import static org.opencds.cqf.cql.evaluator.fhir.AdapterFactory.parametersParametersComponentAdapterFor;

public class LibraryProcessor implements org.opencds.cqf.cql.evaluator.library.api.LibraryProcessor {

    private CqlEvaluator cqlEvaluator;
    private FhirContext fhirContext;
    private LibraryLoader libraryLoader;
    private ParameterParser parameterParser;

    public LibraryProcessor(FhirContext fhirContext, CqlEvaluator cqlEvaluator, LibraryLoader libraryLoader,
            ParameterParser parameterParser) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.cqlEvaluator = Objects.requireNonNull(cqlEvaluator, "cqlEvaluator can not be null");
        this.libraryLoader = Objects.requireNonNull(libraryLoader, "libraryLoader can not be null");
        this.parameterParser = Objects.requireNonNull(parameterParser, "parameterParser can not be null");
    }

    protected IBaseParameters toParameters(EvaluationResult result) {
        IBaseParameters params = null;
        try {
            params = (IBaseParameters) this.fhirContext.getResourceDefinition("Parameters").getImplementingClass()
                    .getConstructor().newInstance();
            ParametersAdapter pa = parametersAdapterFor(params);

            for (Map.Entry<String, Object> entry : result.expressionResults.entrySet()) {
                IBaseBackboneElement ppc = pa.addParameter();

                ParametersParameterComponentAdapter ppca = parametersParametersComponentAdapterFor(ppc);

                ppca.setName(entry.getKey());

                Object value = entry.getValue();

                if (value instanceof IBaseResource) {
                    ppca.setResource((IBaseResource) value);
                    continue;
                }

                if (value instanceof IBaseDatatype) {
                    ppca.setValue((IBaseDatatype) value);
                    continue;
                }

                if (value instanceof CqlType) {
                    ppca.setValue((IBaseDatatype) this.fhirContext.getResourceDefinition("StringType")
                            .getImplementingClass().getConstructor(String.class).newInstance(value.toString()));
                    continue;
                }
            }
        } catch (Exception e) {
            
        }

        return params;
    }

    protected Map<String, Object> toParametersMap(VersionedIdentifier libraryIdentifier, IBaseParameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();

        ParametersAdapter parametersAdapter = parametersAdapterFor(parameters);
        if (parametersAdapter.getParameter() == null) {
            return parameterMap;
        }

        for (IBaseBackboneElement ppc : parametersAdapter.getParameter()) {
            ParametersParameterComponentAdapter ppca = parametersParametersComponentAdapterFor(ppc);
            String name = ppca.getName();
            if (ppca.hasResource()) {
                parameterMap.put(name, ppca.getResource());
            } else if (ppca.hasValue()) {
                parameterMap.put(name, this.parameterParser.parseParameter(this.libraryLoader, libraryIdentifier, name,
                        ppca.getValue().toString()));
            }
        }

        return parameterMap;
    }

    @Override
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