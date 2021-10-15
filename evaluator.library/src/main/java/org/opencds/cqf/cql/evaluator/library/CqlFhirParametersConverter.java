package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseIntegerDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

@Named
public class CqlFhirParametersConverter {

    org.slf4j.Logger logger = LoggerFactory.getLogger(CqlFhirParametersConverter.class);

    protected AdapterFactory adapterFactory;
    protected FhirTypeConverter fhirTypeConverter;
    protected FhirContext fhirContext;
    private IFhirPath fhirPath;

    @Inject
    public CqlFhirParametersConverter(FhirContext fhirContext, AdapterFactory adapterFactory,
            FhirTypeConverter fhirTypeConverter) {
        this.fhirContext = requireNonNull(fhirContext);
        this.adapterFactory = requireNonNull(adapterFactory);
        this.fhirTypeConverter = requireNonNull(fhirTypeConverter);

        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    }

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
                Iterable<?> values = (Iterable<?>) value;
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

    public List<CqlParameterDefinition> toCqlParameterDefinitions(IBaseParameters parameters) {
        if (parameters == null) {
            return Collections.emptyList();
        }

        ParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);

        Map<String, List<ParametersParameterComponentAdapter>> children = parametersAdapter.getParameter().stream()
                .map(x -> this.adapterFactory.createParametersParameters(x)).filter(x -> x.getName() != null)
                .collect(Collectors.groupingBy(ParametersParameterComponentAdapter::getName));

        List<CqlParameterDefinition> cqlParameterDefinitions = new ArrayList<>();
        for (Map.Entry<String, List<ParametersParameterComponentAdapter>> entry : children.entrySet()) {
            // Meta data extension, if present
            Optional<IBaseExtension<?, ?>> ext = entry.getValue().stream().filter(x -> x.hasExtension())
                    .flatMap(x -> x.getExtension().stream())
                    .filter(x -> x.getUrl() != null && x.getUrl()
                            .equals("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition"))
                    .findFirst();

            // Actual values. if present
            List<Object> values = entry.getValue().stream().map(x -> convertToCql(x)).filter(x -> x != null)
            .collect(Collectors.toList());

            String name = entry.getKey();

            Boolean isList = null;
            if (ext.isPresent()) {
                isList = this.isListType(ext.get());
            }

            // Unable to determine via the extension
            // So infer based on the values.
            if (isList == null) {
                if (values.isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("Unable to determine if parameter %s is meant to be collection. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata.",
                                entry.getKey()));
                }
                else if (values.size() == 1) {
                    isList = false;
                }
                else {
                    isList = true;
                }
            }

            if (!isList && entry.getValue().size() > 1) {
                throw new IllegalArgumentException(
                        String.format("The parameter %s was defined as a single value but multiple values were passed",
                                entry.getKey()));
            }

            String type = null;
            if (ext.isPresent()) {
                type = this.getType(ext.get());
            }

            // TODO: This breaks down a bit for CQL System types because they aren't prefixed.
            if (type == null && !values.isEmpty()) {
                type = values.get(0).getClass().getSimpleName();
            }

            if (type == null) {
                throw new IllegalArgumentException(
                    String.format("Unable to infer type for parameter %s. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata.",
                            entry.getKey()));
            }
            
            Object value = null;
            if (isList) 
            {
                value = values;
            }
            else if (!values.isEmpty()) {
                value = values.get(0);
            }

            cqlParameterDefinitions.add(new CqlParameterDefinition(name, type, isList, value));
        }

        return cqlParameterDefinitions;
    }

    public Map<String, Object> toCqlParameters(IBaseParameters parameters) {
        List<CqlParameterDefinition> cqlParameterDefinitions = this.toCqlParameterDefinitions(parameters);
        if (cqlParameterDefinitions == null || cqlParameterDefinitions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> parameterMap = new HashMap<>();
        for (CqlParameterDefinition def : cqlParameterDefinitions) {
            parameterMap.put(def.getName(), def.getValue());
        }

        return parameterMap;
    }

    private String getType(IBaseExtension<?, ?> parameterDefinitionExtension) {
        @SuppressWarnings("rawtypes")
        Optional<IPrimitiveType> type = this.fhirPath.evaluateFirst(parameterDefinitionExtension.getValue(), "type",
                IPrimitiveType.class);
        if (type.isPresent()) {
            return type.get().getValueAsString();
        }

        return null;
    }

    private Boolean isListType(IBaseExtension<?, ?> parameterDefinitionExtension) {
        @SuppressWarnings("rawtypes")
        Optional<IPrimitiveType> max = this.fhirPath.evaluateFirst(parameterDefinitionExtension.getValue(), "max",
                IPrimitiveType.class);
        if (max.isPresent()) {
            String maxString = max.get().getValueAsString();
            if (maxString.equals("1")) {
                return false;
            }

            return true;
        }

        Optional<IBaseIntegerDatatype> min = this.fhirPath.evaluateFirst(parameterDefinitionExtension.getValue(), "min",
                IBaseIntegerDatatype.class);
        if (min.isPresent()) {
            return min.get().getValue() > 1;
        }

        return null;
    }

    private Object convertToCql(ParametersParameterComponentAdapter ppca) {
        if (ppca.hasValue()) {
            return this.fhirTypeConverter.toCqlType(ppca.getValue());
        } else if (ppca.hasResource()) {
            return ppca.getResource();
        } else if (ppca.hasPart()) {
            logger.debug("Ignored {} parameter sub-parts", ppca.getPart().size());
        }

        return null;
    }
}
