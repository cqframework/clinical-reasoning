package org.opencds.cqf.fhir.cql.engine.parameters;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.slf4j.LoggerFactory;

public class CqlFhirParametersConverter {

    org.slf4j.Logger logger = LoggerFactory.getLogger(CqlFhirParametersConverter.class);

    protected IAdapterFactory adapterFactory;
    protected FhirTypeConverter fhirTypeConverter;
    protected FhirContext fhirContext;
    // private IFhirPath fhirPath;
    private ModelResolver modelResolver;

    /*
     * Converts both CQL parameters and CQL Evaluation Results into Fhir Parameters Resources
     */
    public CqlFhirParametersConverter(
            FhirContext fhirContext, IAdapterFactory adapterFactory, FhirTypeConverter fhirTypeConverter) {
        this.fhirContext = requireNonNull(fhirContext);
        this.adapterFactory = requireNonNull(adapterFactory);
        this.fhirTypeConverter = requireNonNull(fhirTypeConverter);
        this.modelResolver = FhirModelResolverCache.resolverForVersion(
                this.fhirContext.getVersion().getVersion());

        // this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    }

    // This is basically a copy and paste from R4FhirTypeConverter, but it's not exposed.
    static final String EMPTY_LIST_EXT_URL = "http://hl7.org/fhir/StructureDefinition/cqf-isEmptyList";
    static final String DATA_ABSENT_REASON_EXT_URL = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
    static final String DATA_ABSENT_REASON_UNKNOWN_CODE = "unknown";

    private static Iterable<?> asIterable(Object value) {
        if (value instanceof Iterable) {
            return (Iterable<?>) value;
        } else {
            return null;
        }
    }

    private static IBaseBooleanDatatype booleanType(FhirContext context, Boolean value) {
        try {
            return (IBaseBooleanDatatype) context.getElementDefinition("Boolean")
                    .getImplementingClass()
                    .getDeclaredConstructor(Boolean.class)
                    .newInstance(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static IBaseDatatype codeType(FhirContext context, String value) {
        try {
            return (IBaseDatatype) context.getElementDefinition("Code")
                    .getImplementingClass()
                    .getDeclaredConstructor(String.class)
                    .newInstance(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static IBaseBooleanDatatype emptyBooleanWithExtension(
            FhirContext context, String url, IBaseDatatype value) {
        var result = booleanType(context, null);
        var ext = ((IBaseHasExtensions) result).addExtension();
        ext.setUrl(url);
        ext.setValue(value);
        return result;
    }

    public IBaseParameters toFhirParameters(EvaluationResult evaluationResult) {
        IBaseParameters params = null;
        try {
            params = (IBaseParameters) this.fhirContext
                    .getResourceDefinition("Parameters")
                    .getImplementingClass()
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            logger.error("Error trying to create Parameters resource", e);
            throw new RuntimeException(e);
        }

        IParametersAdapter pa = this.adapterFactory.createParameters(params);

        for (Map.Entry<String, ExpressionResult> entry : evaluationResult.expressionResults.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue().value();

            if (value == null) {
                // Null value, add a single empty value with an extension indicating the reason
                var dataAbsentValue = emptyBooleanWithExtension(
                        fhirContext,
                        DATA_ABSENT_REASON_EXT_URL,
                        codeType(fhirContext, DATA_ABSENT_REASON_UNKNOWN_CODE));
                addPart(pa, name, dataAbsentValue);
                continue;
            }

            if (value instanceof Iterable) {
                var iterable = asIterable(value);
                if (!iterable.iterator().hasNext()) {
                    // Empty list
                    var emptyListValue =
                            emptyBooleanWithExtension(fhirContext, EMPTY_LIST_EXT_URL, booleanType(fhirContext, true));
                    addPart(pa, name, emptyListValue);
                }
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

    protected IParametersParameterComponentAdapter addPart(IParametersAdapter pa, String name) {
        IBaseBackboneElement ppc = pa.addParameter();
        IParametersParameterComponentAdapter ppca = this.adapterFactory.createParametersParameters(ppc);
        ppca.setName(name);

        return ppca;
    }

    @SuppressWarnings("unchecked")
    protected void addPart(IParametersAdapter pa, String name, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Iterable) {
            var ppca = this.addPart(pa, name);
            Iterable<Object> values = (Iterable<Object>) value;
            for (Object o : values) {
                this.addSubPart(ppca, "element", o);
            }

            return;
        }

        if (this.fhirTypeConverter.isCqlType(value)) {
            value = this.fhirTypeConverter.toFhirType(value);
        }

        // Likely already a parameter part
        if (value instanceof IBaseBackboneElement) {
            var ppca = this.adapterFactory.createParametersParameters((IBaseBackboneElement) value);
            ppca.setName(name);
            pa.addParameter(ppca.get());
        } else if (value instanceof IBaseDatatype) {
            var ppca = this.addPart(pa, name);
            ppca.setValue((IBaseDatatype) value);
        } else if (value instanceof IBaseResource) {
            var ppca = this.addPart(pa, name);
            ppca.setResource((IBaseResource) value);
        } else {
            throw new IllegalArgumentException(String.format(
                    "unknown type when trying to convert to parameters: %s",
                    value.getClass().getSimpleName()));
        }
    }

    protected IParametersParameterComponentAdapter addSubPart(
            IParametersParameterComponentAdapter ppcAdapter, String name) {
        IBaseBackboneElement ppc = ppcAdapter.addPart();
        IParametersParameterComponentAdapter ppca = this.adapterFactory.createParametersParameters(ppc);
        ppca.setName(name);

        return ppca;
    }

    @SuppressWarnings("unchecked")
    protected void addSubPart(IParametersParameterComponentAdapter ppcAdapter, String name, Object value) {
        IParametersParameterComponentAdapter ppca = this.addSubPart(ppcAdapter, name);

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
            throw new IllegalArgumentException(String.format(
                    "unknown type when trying to convert to parameters: %s",
                    value.getClass().getSimpleName()));
        }
    }

    public List<CqlParameterDefinition> toCqlParameterDefinitions(IBaseParameters parameters) {
        if (parameters == null) {
            return Collections.emptyList();
        }

        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);

        Map<String, List<IParametersParameterComponentAdapter>> children = parametersAdapter.getParameter().stream()
                .map(x -> this.adapterFactory.createParametersParameters(x))
                .filter(x -> x.getName() != null)
                .collect(Collectors.groupingBy(IParametersParameterComponentAdapter::getName));

        List<CqlParameterDefinition> cqlParameterDefinitions = new ArrayList<>();
        for (Map.Entry<String, List<IParametersParameterComponentAdapter>> entry : children.entrySet()) {
            // Meta data extension, if present
            Optional<IBaseExtension<?, ?>> ext = entry.getValue().stream()
                    .filter(x -> x.hasExtension())
                    .flatMap(x -> x.getExtension().stream())
                    .filter(x -> x.getUrl() != null
                            && x.getUrl()
                                    .equals("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition"))
                    .findFirst();

            // Actual values. if present
            List<Object> values = entry.getValue().stream()
                    .map(x -> convertToCql(x))
                    .filter(x -> x != null)
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
                    throw new IllegalArgumentException(String.format(
                            "Unable to determine if parameter %s is meant to be collection. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata.",
                            entry.getKey()));
                } else if (values.size() == 1) {
                    isList = false;
                } else {
                    isList = true;
                }
            }

            if (!isList && entry.getValue().size() > 1) {
                throw new IllegalArgumentException(String.format(
                        "The parameter %s was defined as a single value but multiple values were passed",
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
                throw new IllegalArgumentException(String.format(
                        "Unable to infer type for parameter %s. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata.",
                        entry.getKey()));
            }

            Object value = null;
            if (isList) {
                value = values;
            } else if (!values.isEmpty()) {
                value = values.get(0);
            }

            cqlParameterDefinitions.add(new CqlParameterDefinition(name, type, isList, value));
        }

        return cqlParameterDefinitions;
    }

    public Map<String, Object> toCqlParameters(IBaseParameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();
        List<CqlParameterDefinition> cqlParameterDefinitions = this.toCqlParameterDefinitions(parameters);
        if (cqlParameterDefinitions == null || cqlParameterDefinitions.isEmpty()) {
            return parameterMap;
        }

        for (CqlParameterDefinition def : cqlParameterDefinitions) {
            parameterMap.put(def.getName(), def.getValue());
        }

        return parameterMap;
    }

    private String getType(IBaseExtension<?, ?> parameterDefinitionExtension) {
        var type = modelResolver.resolvePath(parameterDefinitionExtension.getValue(), "type");
        // Optional<IPrimitiveType> type = this.fhirPath
        // .evaluateFirst(parameterDefinitionExtension.getValue(), "type", IPrimitiveType.class);
        // if (type.isPresent()) {
        if (type instanceof IPrimitiveType) {
            return ((IPrimitiveType<?>) type).getValueAsString();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Boolean isListType(IBaseExtension<?, ?> parameterDefinitionExtension) {
        var max = modelResolver.resolvePath(parameterDefinitionExtension.getValue(), "max");
        // Optional<IPrimitiveType> max = this.fhirPath
        // .evaluateFirst(parameterDefinitionExtension.getValue(), "max", IPrimitiveType.class);
        // if (max.isPresent()) {
        if (max instanceof IPrimitiveType) {
            String maxString = ((IPrimitiveType<?>) max).getValueAsString();

            return !maxString.equals("1");
        }

        var min = modelResolver.resolvePath(parameterDefinitionExtension.getValue(), "min");
        // Optional<IBaseIntegerDatatype> min = this.fhirPath
        // .evaluateFirst(parameterDefinitionExtension.getValue(), "min", IBaseIntegerDatatype.class);
        // if (min.isPresent()) {
        if (min instanceof IPrimitiveType) {
            return ((IPrimitiveType<Integer>) min).getValue() > 1;
        }

        return false;
    }

    private Object convertToCql(IParametersParameterComponentAdapter ppca) {
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
