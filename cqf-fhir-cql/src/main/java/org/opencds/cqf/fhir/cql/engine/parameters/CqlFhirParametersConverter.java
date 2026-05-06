package org.opencds.cqf.fhir.cql.engine.parameters;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import com.apicatalog.jsonld.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseIntegerDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Decimal;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.cql.engine.runtime.Value;
import org.opencds.cqf.fhir.utility.FhirPathCache;
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
    protected FhirModelResolver<?, ?, ?, ?, ?, ?, ?, ?> modelResolver;
    protected IFhirPath fhirPath;

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
        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
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

        for (Map.Entry<String, ExpressionResult> entry :
                evaluationResult.getExpressionResults().entrySet()) {
            String name = entry.getKey();
            var value = entry.getValue().getValue();

            if (value instanceof org.opencds.cqf.cql.engine.runtime.List list) {
                if (!list.iterator().hasNext()) {
                    // Empty list
                    var emptyListValue =
                            emptyBooleanWithExtension(fhirContext, EMPTY_LIST_EXT_URL, booleanType(fhirContext, true));
                    addPart(pa, name, emptyListValue);
                }
                for (Object o : list) {
                    this.addPart(pa, name, o);
                }
            } else {
                this.addPart(pa, name, value);
            }
        }

        return params;
    }

    protected IParametersParameterComponentAdapter addPart(IParametersAdapter pa, String name) {
        var ppca = pa.addParameter();
        ppca.setName(name);

        return ppca;
    }

    @SuppressWarnings("unchecked")
    protected void addPart(IParametersAdapter pa, String name, Object value) {
        if (value == null) {
            value = emptyBooleanWithExtension(
                    fhirContext, DATA_ABSENT_REASON_EXT_URL, codeType(fhirContext, DATA_ABSENT_REASON_UNKNOWN_CODE));
        }

        value = convertToFhirIfNeeded(value);

        if (value instanceof Tuple tupleValue) {
            var ppca = this.addPart(pa, name);
            tupleValue.getElements().forEach((k, v) -> addSubPart(ppca, k, v));

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
            value = this.fhirTypeConverter.toFhirType((Value) value);
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
        var ppca = ppcAdapter.addPart();
        ppca.setName(name);

        return ppca;
    }

    @SuppressWarnings("unchecked")
    protected void addSubPart(IParametersParameterComponentAdapter ppcAdapter, String name, Object value) {
        IParametersParameterComponentAdapter ppca = this.addSubPart(ppcAdapter, name);

        if (value == null) {
            return;
        }

        value = convertToFhirIfNeeded(value);

        if (value instanceof Iterable) {
            Iterable<Object> values = (Iterable<Object>) value;
            for (Object o : values) {
                this.addSubPart(ppca, "element", o);
            }

            return;
        }

        if (this.fhirTypeConverter.isCqlType(value)) {
            value = this.fhirTypeConverter.toFhirType((Value) value);
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
        // This list needs to be mutable so that extra parameter definitions can be added if needed.
        List<CqlParameterDefinition> cqlParameterDefinitions = new ArrayList<>();
        if (parameters == null) {
            return cqlParameterDefinitions;
        }

        IParametersAdapter parametersAdapter = this.adapterFactory.createParameters(parameters);

        Map<String, List<IParametersParameterComponentAdapter>> children = parametersAdapter.getParameter().stream()
                .filter(x -> x.getName() != null)
                .collect(Collectors.groupingBy(IParametersParameterComponentAdapter::getName));

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
            var values = entry.getValue().stream()
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
                var firstValue = values.get(0);
                if (firstValue instanceof ClassInstance cci) {
                    type = "FHIR." + cci.getType().getLocalPart();
                } else {
                    type = firstValue.getClass().getSimpleName();
                }
            }

            if (type == null) {
                throw new IllegalArgumentException(
                        "Unable to infer type for parameter %s. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata."
                                .formatted(entry.getKey()));
            }

            Value value = null;
            if (isList) {
                value = new org.opencds.cqf.cql.engine.runtime.List(values);
            } else if (!values.isEmpty()) {
                value = values.get(0);
            }

            cqlParameterDefinitions.add(new CqlParameterDefinition(name, type, isList, value));
        }

        return cqlParameterDefinitions;
    }

    public Map<String, Value> toCqlParameters(IBaseParameters parameters) {
        Map<String, Value> parameterMap = new HashMap<>();
        List<CqlParameterDefinition> cqlParameterDefinitions = this.toCqlParameterDefinitions(parameters);
        if (cqlParameterDefinitions == null || cqlParameterDefinitions.isEmpty()) {
            return parameterMap;
        }

        for (CqlParameterDefinition def : cqlParameterDefinitions) {
            parameterMap.put(def.getName(), def.getValue());
        }

        return parameterMap;
    }

    public Map<String, Value> toCqlParameters(Map<String, Object> parameters) {
        Map<String, Value> parameterMap = new HashMap<>();
        parameters.forEach((k, v) -> {
            var className = v.getClass().getName();
            Value value;
            if (v instanceof List<?> list) {
                value = new org.opencds.cqf.cql.engine.runtime.List(
                        list.stream().map(this::convertToCqlIfNeeded).toList());
            } else if (className.contains("org.hl7.fhir") && className.contains("Tuple")) {
                Map<String, Value> elements = adapterFactory.createTuple((IBase) v).getProperties().entrySet().stream()
                        .map(entry -> {
                            var listValue = ((List<?>) entry.getValue())
                                    .stream()
                                            .map(e -> modelResolver.toCqlValue(e, false))
                                            .toList();
                            var cqlValue = listValue.size() != 1
                                    ? new org.opencds.cqf.cql.engine.runtime.List(listValue)
                                    : listValue.get(0);
                            return Map.entry(entry.getKey(), cqlValue);
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                value = new Tuple().withElements(elements);
            } else {
                value = convertToCqlIfNeeded(v);
            }
            parameterMap.put(k, value);
        });
        return parameterMap;
    }

    private Value convertToCqlIfNeeded(Object value) {
        var className = value.getClass().getName();
        return className.contains("org.hl7.fhir") ? modelResolver.toCqlValue(value, false) : (Value) value;
    }

    @SuppressWarnings("rawtypes")
    private String getType(IBaseExtension<?, ?> parameterDefinitionExtension) {
        Optional<IPrimitiveType> type =
                this.fhirPath.evaluateFirst(parameterDefinitionExtension.getValue(), "type", IPrimitiveType.class);
        return type.map(IPrimitiveType::getValueAsString).orElse(null);
    }

    @SuppressWarnings({"rawtypes"})
    private Boolean isListType(IBaseExtension<?, ?> parameterDefinitionExtension) {
        Optional<IPrimitiveType> max =
                this.fhirPath.evaluateFirst(parameterDefinitionExtension.getValue(), "max", IPrimitiveType.class);
        if (max.isPresent()) {
            var maxString = max.get().getValueAsString();

            return !maxString.equals("1");
        }

        Optional<IBaseIntegerDatatype> min =
                this.fhirPath.evaluateFirst(parameterDefinitionExtension.getValue(), "min", IBaseIntegerDatatype.class);
        return min.filter(iBaseIntegerDatatype -> iBaseIntegerDatatype.getValue() > 1)
                .isPresent();
    }

    public Object convertToFhirIfNeeded(Object value) {
        return value instanceof ClassInstance classInstance
                        && classInstance.getType().getNamespaceURI().equals(FhirModelResolver.fhirModelNamespaceUri)
                ? toFhirValue(classInstance)
                : value;
    }

    public IBase toFhirValue(ClassInstance classInstance) {
        return toFhirValue(classInstance, null);
    }

    public IBase toFhirValue(ClassInstance classInstance, String parentName) {
        var typeName = classInstance.getType().getLocalPart();
        var clazz = modelResolver.resolveType(typeName);
        if (clazz == null) {
            throw new IllegalArgumentException("Could not resolve FHIR type: " + typeName);
        }
        if (StringUtils.isNotBlank(parentName)
                && clazz.getName().contains("$")
                && !clazz.getEnclosingClass().getSimpleName().equals(parentName)
                && !clazz.getEnclosingClass().getSimpleName().equals("Enumerations")) {
            var correctClassName =
                    clazz.getName().replace(clazz.getEnclosingClass().getSimpleName(), parentName);
            try {
                clazz = Class.forName(correctClassName);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Could not resolve inner FHIR type: " + typeName);
            }
        }

        IBase instance;
        try {
            if (clazz.isEnum()) {
                instance = (IBase) modelResolver.createHapiInstance$engine_fhir(typeName);
            } else {
                instance = (IBase) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create instance of " + typeName, e);
        }

        if (instance instanceof IBaseEnumeration<?> enumeration) {
            var value = classInstance.get("value");
            if (value != null) {
                enumeration.setValueAsString(value.toString());
            }
            return instance;
        }

        if (instance instanceof IPrimitiveType<?> primitive) {
            var value = classInstance.get("value");
            if (value instanceof DateTime dateTime) {
                modelResolver.setPrimitiveValue(dateTime, primitive);
            } else if (value instanceof Date date) {
                modelResolver.setPrimitiveValue(date, primitive);
            } else if (value instanceof org.opencds.cqf.cql.engine.runtime.Boolean bool) {
                modelResolver.setPrimitiveValue(bool.getValue(), primitive);
            } else if (value instanceof org.opencds.cqf.cql.engine.runtime.Integer integer) {
                modelResolver.setPrimitiveValue(integer.getValue(), primitive);
            } else if (value instanceof Decimal decimal) {
                modelResolver.setPrimitiveValue(decimal.getValue(), primitive);
            } else if (value != null) {
                modelResolver.setPrimitiveValue(value.toString(), primitive);
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        var ibaseClazz = (Class<? extends IBase>) clazz;
        var definition = (BaseRuntimeElementCompositeDefinition<?>) fhirContext.getElementDefinition(ibaseClazz);
        if (definition == null) {
            @SuppressWarnings("unchecked")
            var resourceClazz = (Class<? extends IBaseResource>) clazz;
            definition = fhirContext.getResourceDefinition(resourceClazz);
        }

        for (var child : definition.getChildren()) {
            var elementValue = classInstance.get(child.getElementName());
            if (elementValue == null) {
                continue;
            }
            if (elementValue instanceof org.opencds.cqf.cql.engine.runtime.List list) {
                for (var item : list) {
                    if (item instanceof ClassInstance childCci) {
                        child.getMutator().addValue(instance, toFhirValue(childCci, typeName));
                    }
                }
            } else if (elementValue instanceof ClassInstance childCci) {
                child.getMutator().addValue(instance, toFhirValue(childCci, typeName));
            }
        }
        return instance;
    }

    private Value convertToCql(IParametersParameterComponentAdapter ppca) {
        if (ppca.hasValue()) {
            return (Value) this.fhirTypeConverter.toCqlType(ppca.getValue());
        } else if (ppca.hasResource()) {
            return modelResolver.toCqlValue(ppca.getResource(), false);
        } else if (ppca.hasPart()) {
            logger.debug("Ignored {} parameter sub-parts", ppca.getPart().size());
        }

        return null;
    }
}
