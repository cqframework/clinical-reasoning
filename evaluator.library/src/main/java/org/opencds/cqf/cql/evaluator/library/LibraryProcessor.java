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
import org.opencds.cqf.cql.evaluator.ParameterParser;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter;

import ca.uhn.fhir.context.FhirContext;

class LibraryProcessor {

    private CqlEvaluator cqlEvaluator;
    private FhirContext fhirContext;
    private LibraryLoader libraryLoader;
    private ParameterParser parameterParser;
    private AdapterFactory adapterFactory;

    LibraryProcessor(FhirContext fhirContext, AdapterFactory adapterFactory, CqlEvaluator cqlEvaluator, LibraryLoader libraryLoader,
            ParameterParser parameterParser) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.cqlEvaluator = Objects.requireNonNull(cqlEvaluator, "cqlEvaluator can not be null");
        this.libraryLoader = Objects.requireNonNull(libraryLoader, "libraryLoader can not be null");
        this.parameterParser = Objects.requireNonNull(parameterParser, "parameterParser can not be null");
        this.adapterFactory = Objects.requireNonNull(adapterFactory, "adapterFactory can not be null");
    }

    protected IBaseParameters toParameters(EvaluationResult result) {
        IBaseParameters params = null;
        try {
            params = (IBaseParameters) this.fhirContext.getResourceDefinition("Parameters").getImplementingClass()
                    .getConstructor().newInstance();
            ParametersAdapter pa = this.adapterFactory.createParameters(params);

            for (Map.Entry<String, Object> entry : result.expressionResults.entrySet()) {
                IBaseBackboneElement ppc = pa.addParameter();

                ParametersParameterComponentAdapter parametersComponent = this.adapterFactory.createParametersParameters(ppc);

                parametersComponent.setName(entry.getKey());

                Object value = entry.getValue();

                if (value instanceof IBaseResource) {
                    parametersComponent.setResource((IBaseResource) value);
                    continue;
                }

                if (value instanceof IBaseDatatype) {
                    parametersComponent.setValue((IBaseDatatype) value);
                    continue;
                }

                if (value instanceof CqlType) {
                    parametersComponent.setValue((IBaseDatatype) this.fhirContext.getResourceDefinition("StringType")
                            .getImplementingClass().getConstructor(String.class).newInstance(value.toString()));
                    continue;
                }

                parametersComponent.setValue((IBaseDatatype) this.fhirContext.getResourceDefinition("StringType")
                .getImplementingClass().getConstructor(String.class).newInstance(value.toString()));
            }
        } catch (Exception e) {

        }

        return params;
    }

    protected Map<String, Object> toParametersMap(VersionedIdentifier libraryIdentifier, IBaseParameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();

        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);
        if (parametersAdapter.getParameter() == null) {
            return parameterMap;
        }

        for (IBaseBackboneElement ppc : parametersAdapter.getParameter()) {
            ParametersParameterComponentAdapter parametersComponent = this.adapterFactory.createParametersParameters(ppc);
            String name = parametersComponent.getName();
            if (parametersComponent.hasResource()) {
                parameterMap.put(name, parametersComponent.getResource());
            } else if (parametersComponent.hasValue()) {
                parameterMap.put(name, this.parameterParser.parseParameter(this.libraryLoader, libraryIdentifier, name,
                        parametersComponent.getValue().toString()));
            }
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