package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

@Named
public class CqlFhirParametersConverter {

    org.slf4j.Logger logger = LoggerFactory.getLogger(CqlFhirParametersConverter.class);

    protected AdapterFactory adapterFactory;
    protected FhirTypeConverter fhirTypeConverter;
    protected FhirContext fhirContext;

    @Inject
    public CqlFhirParametersConverter(FhirContext fhirContext, AdapterFactory adapterFactory,
            FhirTypeConverter fhirTypeConverter) {
        this.fhirContext = requireNonNull(fhirContext);
        this.adapterFactory = requireNonNull(adapterFactory);
        this.fhirTypeConverter = requireNonNull(fhirTypeConverter);
    }

    @SuppressWarnings("unchecked")
    public IBaseParameters toFhirParameters(EvaluationResult evaluationResult) {
        IBaseParameters params = null;
        try {
            params = (IBaseParameters) this.fhirContext.getResourceDefinition("Parameters").getImplementingClass()
                    .getConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Error trying to create Parameters resource", e);
            throw new RuntimeException(e);
        }

        ParametersAdapter pa = this.adapterFactory.createParameters(params);

        for (Map.Entry<String, Object> entry : evaluationResult.expressionResults.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                this.addPart(pa, name);
                continue;
            }

            if (value instanceof Iterable) {
                Iterable<Object> values = (Iterable<Object>) value;
                for (Object o : values) {
                    this.addPart(pa, name, o);
                }
            } else {
                this.addPart(pa, name, value);
            }
        }

        return params;
    }

    protected ParametersParameterComponentAdapter addPart(ParametersAdapter pa, String name) {
        IBaseBackboneElement ppc = pa.addParameter();
        ParametersParameterComponentAdapter ppca = this.adapterFactory.createParametersParameters(ppc);
        ppca.setName(name);

        return ppca;
    }

    @SuppressWarnings("unchecked")
    protected void addPart(ParametersAdapter pa, String name, Object value) {
        ParametersParameterComponentAdapter ppca = this.addPart(pa, name);

        if (value == null) {
            return;
        }

        if (value instanceof Iterable) {
            Iterable<Object> values = (Iterable<Object>) value;
            for (Object o : values) {
                this.addSubPart(ppca, "element", o);
            }

            return;
        }

        if (this.fhirTypeConverter.isCqlType(value)) {
            value = this.fhirTypeConverter.toFhirType(value);
        }

        if (value instanceof IBaseDatatype) {
            ppca.setValue((IBaseDatatype) value);
        } else if (value instanceof IBaseResource) {
            ppca.setResource((IBaseResource) value);
        } else {
            throw new IllegalArgumentException(String.format("unknown type when trying to convert to parameters: %s",
                    value.getClass().getSimpleName()));
        }
    }

    protected ParametersParameterComponentAdapter addSubPart(ParametersParameterComponentAdapter ppcAdapter,
            String name) {
        IBaseBackboneElement ppc = ppcAdapter.addPart();
        ParametersParameterComponentAdapter ppca = this.adapterFactory.createParametersParameters(ppc);
        ppca.setName(name);

        return ppca;
    }

    @SuppressWarnings("unchecked")
    protected void addSubPart(ParametersParameterComponentAdapter ppcAdapter, String name, Object value) {
        ParametersParameterComponentAdapter ppca = this.addSubPart(ppcAdapter, name);

        if (value == null) {
            return;
        }

        if (value instanceof Iterable) {
            Iterable<Object> values = (Iterable<Object>) value;
            for (Object o : values) {
                this.addSubPart(ppca, "element", o);
            }

            return;
        }

        if (this.fhirTypeConverter.isCqlType(value)) {
            value = this.fhirTypeConverter.toFhirType(value);
        }

        if (value instanceof IBaseDatatype) {
            ppca.setValue((IBaseDatatype) value);
        } else if (value instanceof IBaseResource) {
            ppca.setResource((IBaseResource) value);
        } else {
            throw new IllegalArgumentException(String.format("unknown type when trying to convert to parameters: %s",
                    value.getClass().getSimpleName()));
        }
    }

    public Map<String, Object> toCqlParameters(IBaseParameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();

        if (parameters == null) {
            return parameterMap;
        }

        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);

        Map<String,List<ParametersParameterComponentAdapter>> children = parametersAdapter.getParameter().stream()
                .map(x -> this.adapterFactory.createParametersParameters(x)).collect(Collectors.groupingBy(ParametersParameterComponentAdapter::getName));

        

        for (Map.Entry<String,List<ParametersParameterComponentAdapter>> entry : children.entrySet()) {
            // Only know how to convert values
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                parameterMap.put(entry.getKey(), null);
            }
            else if (entry.getValue().size() == 1) {
                ParametersParameterComponentAdapter ppca = entry.getValue().get(0);
                parameterMap.put(entry.getKey(), convertToCql(ppca));
            }
            else {
                List<Object> values = new ArrayList<>();
                for (ParametersParameterComponentAdapter ppca : entry.getValue()) {
                    values.add(convertToCql(ppca));
                }

                parameterMap.put(entry.getKey(), values);
            }
        }

        return parameterMap;
    }

    private Object convertToCql(ParametersParameterComponentAdapter ppca) {
        if (ppca.hasValue()) {
            return this.fhirTypeConverter.toCqlType(ppca.getValue());
        }
        else if (ppca.hasResource()) {
            return ppca.getResource();
        }
        else if (ppca.hasPart()) {
            logger.debug("Ignored {} parameter sub-parts", ppca.getPart().size());
        }

        return null;
    }
}
