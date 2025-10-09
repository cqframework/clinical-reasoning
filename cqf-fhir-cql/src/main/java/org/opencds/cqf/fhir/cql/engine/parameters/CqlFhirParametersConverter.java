package org.opencds.cqf.fhir.cql.engine.parameters;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
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
    private final ModelResolver modelResolver;

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

    private static IBaseBooleanDatatype booleanType(FhirContext context, Boolean value) {
        try {
            var booleanElementDef = context.getElementDefinition("Boolean");
            if (booleanElementDef == null) {
                throw new InternalErrorException("Unable to get definition for Boolean element");
            }
            return (IBaseBooleanDatatype) booleanElementDef.newInstance(value);
        } catch (Exception e) {
            throw new InternalErrorException("error creating BooleanType", e);
        }
    }

    private static IBaseDatatype codeType(FhirContext context, String value) {
        try {
            var codeElementDef = context.getElementDefinition("Code");
            if (codeElementDef == null) {
                throw new InternalErrorException("Unable to get definition for Code element");
            }
            return (IBaseDatatype) codeElementDef.newInstance(value);
        } catch (Exception e) {
            throw new InternalErrorException("error creating CodeType", e);
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

            if (value instanceof Iterable<?> iterable) {
                if (!iterable.iterator().hasNext()) {
                    // Empty list
                    var emptyListValue =
                            emptyBooleanWithExtension(fhirContext, EMPTY_LIST_EXT_URL, booleanType(fhirContext, true));
                    addPart(pa, name, emptyListValue);
                }
                for (Object o : iterable) {
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
        IParametersParameterComponentAdapter ppca = this.adapterFactory.createParametersParameter(ppc);
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

        if (value instanceof IBaseDatatype dataType) {
            var ppca = this.addPart(pa, name);
            ppca.setValue(dataType);
        } else if (value instanceof IBaseBackboneElement backboneValue) {
            // Likely already a parameter part
            var ppca = this.adapterFactory.createParametersParameter(backboneValue);
            ppca.setName(name);
            pa.addParameter(ppca.get());
        } else if (value instanceof IBaseResource resource) {
            var ppca = this.addPart(pa, name);
            ppca.setResource(resource);
        } else {
            throw new IllegalArgumentException("unknown type when trying to convert to parameters: %s"
                    .formatted(value.getClass().getSimpleName()));
        }
    }

    protected IParametersParameterComponentAdapter addSubPart(
            IParametersParameterComponentAdapter ppcAdapter, String name) {
        IBaseBackboneElement ppc = ppcAdapter.addPart();
        IParametersParameterComponentAdapter ppca = this.adapterFactory.createParametersParameter(ppc);
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

        if (value instanceof IBaseDatatype datatype) {
            ppca.setValue(datatype);
        } else if (value instanceof IBaseResource resource) {
            ppca.setResource(resource);
        } else {
            throw new IllegalArgumentException("unknown type when trying to convert to parameters: %s"
                    .formatted(value.getClass().getSimpleName()));
        }
    }

    public List<CqlParameterDefinition> toCqlParameterDefinitions(IBaseParameters parameters) {
        if (parameters == null) {
            return Collections.emptyList();
        }

        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);

        Map<String, List<IParametersParameterComponentAdapter>> children = parametersAdapter.getParameter().stream()
                .filter(x -> x.getName() != null)
                .collect(Collectors.groupingBy(IParametersParameterComponentAdapter::getName));

        List<CqlParameterDefinition> cqlParameterDefinitions = new ArrayList<>();
        for (Map.Entry<String, List<IParametersParameterComponentAdapter>> entry : children.entrySet()) {
            // Meta data extension, if present
            Optional<IBaseExtension<?, ?>> ext = entry.getValue().stream()
                    .filter(IAdapter::hasExtension)
                    .flatMap(x -> x.getExtension().stream())
                    .filter(x -> x.getUrl() != null
                            && x.getUrl()
                                    .equals("http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition"))
                    .findFirst();

            // Actual values. if present
            List<Object> values = entry.getValue().stream()
                    .map(this::convertToCql)
                    .filter(Objects::nonNull)
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
                            "Unable to determine if parameter %s is meant to be collection. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata."
                                    .formatted(entry.getKey()));
                } else {
                    isList = values.size() != 1;
                }
            }

            if (!isList && entry.getValue().size() > 1) {
                throw new IllegalArgumentException(
                        "The parameter %s was defined as a single value but multiple values were passed"
                                .formatted(entry.getKey()));
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
                        "Unable to infer type for parameter %s. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata."
                                .formatted(entry.getKey()));
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
        if (type instanceof IPrimitiveType<?> primitiveType) {
            return primitiveType.getValueAsString();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Boolean isListType(IBaseExtension<?, ?> parameterDefinitionExtension) {
        var max = modelResolver.resolvePath(parameterDefinitionExtension.getValue(), "max");
        // Optional<IPrimitiveType> max = this.fhirPath
        // .evaluateFirst(parameterDefinitionExtension.getValue(), "max", IPrimitiveType.class);
        // if (max.isPresent()) {
        if (max instanceof IPrimitiveType<?> type) {
            String maxString = type.getValueAsString();

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
