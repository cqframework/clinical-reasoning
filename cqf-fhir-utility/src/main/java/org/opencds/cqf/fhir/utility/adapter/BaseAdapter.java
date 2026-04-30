package org.opencds.cqf.fhir.utility.adapter;

import static java.lang.Integer.parseInt;
import static org.opencds.cqf.fhir.utility.adapter.AdapterHelper.as;

import ca.uhn.fhir.context.BaseRuntimeChildDatatypeDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeChildChoiceDefinition;
import ca.uhn.fhir.context.RuntimeChildPrimitiveDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeChildPrimitiveEnumerationDatatypeDefinition;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeDefinition;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public abstract class BaseAdapter {
    protected static final String ENUMERATION = "Enumeration";
    protected static final Pattern EXTENSION_PATTERN = Pattern.compile("extension\\('([^']+)'\\)(\\[(\\d+)])?");

    protected final FhirContext fhirContext;
    protected final FhirVersionEnum fhirVersion;
    protected final IAdapterFactory adapterFactory;

    protected record ExtensionInfo(String url, int index) {}

    public BaseAdapter(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        fhirVersion = this.fhirContext.getVersion().getVersion();
        adapterFactory = IAdapterFactory.forFhirContext(this.fhirContext);
    }

    public FhirContext fhirContext() {
        return fhirContext;
    }

    public IAdapterFactory getAdapterFactory() {
        return adapterFactory;
    }

    public Object resolvePath(Object target, String path) {
        String[] identifiers = path.split("\\.");
        for (String identifier : identifiers) {
            // handling indexes: i.e. item[0].code
            if (identifier.contains("[")) {
                int index = Character.getNumericValue(identifier.charAt(identifier.indexOf("[") + 1));
                target = resolveProperty(target, identifier.replaceAll("\\[\\d]", ""));
                target = ((ArrayList<?>) target).get(index);
            } else {
                target = resolveProperty(target, identifier);
            }
        }

        return target;
    }

    protected Object resolveProperty(Object target, String path) {
        if (target == null) {
            return null;
        }

        if (target instanceof IBaseEnumeration && path.equals("value")) {
            return ((IBaseEnumeration<?>) target).getValueAsString();
        }

        if (target instanceof IAnyResource resource && resource.fhirType().equals(path)) {
            return target;
        }

        if (target instanceof List<?> list) {
            var index = 0;
            if (path.contains("[\\d]")) {
                try {
                    index = parseInt(path.substring(path.indexOf("[")).replace("]", ""));
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }
            target = list.get(index);
        }

        IBase base = (IBase) target;
        BaseRuntimeElementCompositeDefinition<?> definition;
        if (base instanceof IPrimitiveType) {
            return path.equals("value") ? ((IPrimitiveType<?>) target).getValue() : target;
        } else {
            definition = resolveRuntimeDefinition(base);
        }

        BaseRuntimeChildDefinition child = definition.getChildByName(path);
        if (child == null) {
            child = resolveChoiceProperty(definition, path);
        }

        if (child == null) {
            return null;
        }

        List<IBase> values = child.getAccessor().getValues(base);

        if (values == null || values.isEmpty()) {
            return null;
        }

        // If the instance is a primitive (including (or even especially an enumeration), and it has no value, return
        // null
        if (child instanceof RuntimeChildPrimitiveDatatypeDefinition) {
            IBase value = values.get(0);
            if (value instanceof IPrimitiveType) {
                if (!((IPrimitiveType<?>) value).hasValue()) {
                    return null;
                }
            }
        }

        if (child instanceof RuntimeChildChoiceDefinition
                && !child.getElementName().equalsIgnoreCase(path)) {
            if (!values.get(0)
                    .getClass()
                    .getSimpleName()
                    .equalsIgnoreCase(
                            child.getChildByName(path).getImplementingClass().getSimpleName())) {
                return null;
            }
        }

        return child.getMax() < 1 ? values : values.get(0);
    }

    public void setValue(IBase target, String path, Object value) {
        if (target == null) {
            return;
        }

        var definition = resolveRuntimeDefinition(target);
        if (path.contains(("."))) {
            setNestedValue(target, path, value, definition);
        } else {
            if (!path.contains("[x]")) {
                var childDef = definition.getChildByName(path);
                if (childDef != null) {
                    var elementDef = childDef.getChildByName(path);
                    if (elementDef != null
                            && elementDef.getImplementingClass().getSimpleName().equals(ENUMERATION)
                            && value != null
                            && !value.getClass().getSimpleName().equals(ENUMERATION)) {
                        value = getEnumValue((RuntimeChildPrimitiveEnumerationDatatypeDefinition) childDef, value);
                    }
                }
            }

            if (target instanceof IBaseEnumeration<?> enumeration && path.equals("value")) {
                enumeration.setValueAsString((String) value);
                return;
            }

            if (target instanceof IPrimitiveType<?> primitiveType) {
                setPrimitiveValue(value, primitiveType);
                return;
            }

            BaseRuntimeChildDefinition child = definition.getChildByName(path);
            if (child == null) {
                child = resolveChoiceProperty(definition, path);
            }

            if (child == null) {
                throw new IllegalArgumentException(String.format("Unable to resolve path %s.", path));
            }

            try {
                if (value instanceof Iterable) {
                    for (Object val : (Iterable<?>) value) {
                        child.getMutator().addValue(target, setBaseValue(val, target, getChildType(child)));
                    }
                } else {
                    child.getMutator().setValue(target, setBaseValue(value, target, getChildType(child)));
                }
            } catch (IllegalArgumentException le) {
                throw new UnprocessableEntityException(
                        String.format("Configuration error encountered: %s", le.getMessage()));
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void setPrimitiveValue(Object value, IPrimitiveType target) {
        String simpleName = target.getClass().getSimpleName();
        switch (simpleName) {
            case "DateTimeType":
            case "InstantType":
                target.setValueAsString(value.toString());
                break;
            case "TimeType":
                target.setValue(value.toString());
                break;
            case "Base64BinaryType":
                target.setValueAsString((String) value);
                break;
            default:
                target.setValue(value);
        }
    }

    protected IBase setBaseValue(Object value, IBase target, Class<?> type) {
        if (target instanceof IPrimitiveType<?> primitiveType) {
            setPrimitiveValue(value, primitiveType);
        }
        return (IBase) (type == null ? value : as(fhirVersion, value, type));
    }

    protected Class<?> getChildType(BaseRuntimeChildDefinition child) {
        if (child instanceof BaseRuntimeChildDatatypeDefinition datatypeDefinition) {
            return datatypeDefinition.getDatatype();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T extends IBase> BaseRuntimeElementCompositeDefinition<T> resolveRuntimeDefinition(IBase base) {
        if (base instanceof IAnyResource resource) {
            return (BaseRuntimeElementCompositeDefinition<T>) fhirContext.getResourceDefinition(resource);
        } else if (base instanceof IBaseBackboneElement || base instanceof IBaseElement) {
            return (BaseRuntimeElementCompositeDefinition<T>) fhirContext.getElementDefinition(base.getClass());
        } else if (base instanceof ICompositeType) {
            return (BaseRuntimeElementCompositeDefinition<T>) fhirContext.getElementDefinition(base.getClass());
        }

        throw new UnprocessableEntityException("Unable to resolve the runtime definition for %s"
                .formatted(base.getClass().getName()));
    }

    protected BaseRuntimeChildDefinition resolveChoiceProperty(
            BaseRuntimeElementCompositeDefinition<?> definition, String path) {
        for (Object child : definition.getChildren()) {
            if (child instanceof RuntimeChildChoiceDefinition choiceDefinition) {
                if (choiceDefinition.getElementName().startsWith(path)) {
                    return choiceDefinition;
                }
            }
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T extends Enum<?>, E extends IBaseEnumeration<T>> E getEnumValue(
            RuntimeChildPrimitiveEnumerationDatatypeDefinition targetDef, Object value) {
        String enumValue;
        if (value instanceof IPrimitiveType<?> primitiveType) {
            enumValue = primitiveType.getValueAsString();
        } else if (value instanceof IBaseCoding coding) {
            enumValue = coding.getCode();
        } else {
            enumValue = value.toString();
        }
        return switch (fhirContext.getVersion().getVersion()) {
            case DSTU3 ->
                (E) new org.hl7.fhir.dstu3.model.Enumeration(toEnumFactory(targetDef.getBoundEnumType()), enumValue);
            case R4 ->
                (E) new org.hl7.fhir.r4.model.Enumeration(toEnumFactory(targetDef.getBoundEnumType()), enumValue);
            case R5 ->
                (E) new org.hl7.fhir.r5.model.Enumeration(toEnumFactory(targetDef.getBoundEnumType()), enumValue);
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    static <E extends IBaseEnumFactory<?>> E toEnumFactory(Class<?> enumerationType) {
        Class<?> clazz;
        String className = enumerationType.getName() + "EnumFactory";
        E retVal;
        try {
            clazz = Class.forName(className);
            retVal = (E) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new UnprocessableEntityException("Failed to instantiate %s".formatted(className));
        }
        return retVal;
    }

    public void setNestedValue(IBase target, String path, Object value, BaseRuntimeElementDefinition<?> def) {
        var segments = splitPathSegments(path);
        for (int i = 0; i < segments.size(); i++) {
            var segment = segments.get(i);
            var isLast = i == segments.size() - 1;

            if (isExtensionSegment(segment)) {
                var extInfo = parseExtensionSegment(segment);
                target = resolveOrCreateExtension(target, extInfo);
                if (!isLast) {
                    def = fhirContext.getElementDefinition(target.getClass());
                }
            } else {
                var isList = segment.contains("[");
                var isSlice = segment.contains(":");
                var sliceName = isSlice ? segment.split(":")[1] : null;
                var index = isList ? Character.getNumericValue(segment.charAt(segment.indexOf("[") + 1)) : 0;
                var targetPath = getTargetPath(segment, isList, isSlice);
                var targetDef = def.getChildByName(targetPath);
                if (targetDef != null) {
                    var targetValues = targetDef.getAccessor().getValues(target);
                    var targetValue = (targetValues.size() >= index + 1 && !isLast)
                            ? getTargetValueFromList(sliceName, index, targetValues)
                            : getTargetValue(target, value, isLast, targetPath, targetDef);
                    target = targetValue == null ? target : targetValue;
                    if (!isLast) {
                        var nextDef = fhirContext.getElementDefinition(target.getClass());
                        if (nextDef instanceof BaseRuntimeElementCompositeDefinition<?>) def = nextDef;
                        else if (nextDef instanceof RuntimePrimitiveDatatypeDefinition) def = nextDef;
                        else
                            throw new UnprocessableEntityException("Unable to resolve the runtime definition for %s"
                                    .formatted(target.getClass().getName()));
                    }
                }
            }
        }
    }

    protected static List<String> splitPathSegments(String path) {
        var segments = new ArrayList<String>();
        var current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '.' && !inQuotes) {
                segments.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            segments.add(current.toString());
        }
        return segments;
    }

    protected static boolean isExtensionSegment(String segment) {
        return EXTENSION_PATTERN.matcher(segment).matches();
    }

    protected static ExtensionInfo parseExtensionSegment(String segment) {
        var matcher = EXTENSION_PATTERN.matcher(segment);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not an extension segment: " + segment);
        }
        var url = matcher.group(1);
        var index = matcher.group(3) != null ? parseInt(matcher.group(3)) : 0;
        return new ExtensionInfo(url, index);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected IBase resolveOrCreateExtension(IBase target, ExtensionInfo info) {
        if (!(target instanceof IBaseHasExtensions hasExtensions)) {
            throw new IllegalArgumentException(
                    "Target does not support extensions: " + target.getClass().getName());
        }
        var matching = hasExtensions.getExtension().stream()
                .filter(ext -> info.url().equals(ext.getUrl()))
                .toList();
        if (matching.size() > info.index()) {
            return matching.get(info.index());
        }
        var extensionList = (List) hasExtensions.getExtension();
        IBaseExtension<?, ?> created = null;
        for (int i = matching.size(); i <= info.index(); i++) {
            created = newExtension(info.url());
            extensionList.add(created);
        }
        return created;
    }

    protected IBaseExtension<?, ?> newExtension(String url) {
        return switch (fhirContext.getVersion().getVersion()) {
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Extension(url);
            case R4 -> new org.hl7.fhir.r4.model.Extension(url);
            case R5 -> new org.hl7.fhir.r5.model.Extension(url);
            default ->
                throw new IllegalStateException(
                        "Unsupported FHIR version: " + fhirContext.getVersion().getVersion());
        };
    }

    protected String getTargetPath(String identifier, boolean isList, boolean isSlice) {
        if (isList) {
            return identifier.replaceAll("\\[\\d]", "");
        }
        if (isSlice) {
            return identifier.substring(0, identifier.indexOf(":"));
        }
        return identifier;
    }

    protected IBase getTargetValue(
            IBase target, Object value, boolean isLast, String targetPath, BaseRuntimeChildDefinition targetDef) {
        IBase targetValue = null;
        var elementDef = targetDef.getChildByName(targetPath);
        if (isLast) {
            var elementClass = elementDef.getImplementingClass();
            if (elementClass.getSimpleName().equals(ENUMERATION)) {
                if (as(fhirVersion, value, IPrimitiveType.class) instanceof IPrimitiveType<?> primitiveType) {
                    targetValue = getEnumValue(
                            (RuntimeChildPrimitiveEnumerationDatatypeDefinition) targetDef,
                            primitiveType.getValueAsString());
                }
            } else {
                targetValue = (IBase) as(fhirVersion, value, elementClass);
            }
        } else {
            targetValue = elementDef.newInstance(targetDef.getInstanceConstructorArguments());
        }
        if (targetValue != null) {
            targetDef.getMutator().addValue(target, targetValue);
        }
        return targetValue;
    }

    protected IBase getTargetValueFromList(String sliceName, int index, List<IBase> targetValues) {
        IBase targetValue;
        if (targetValues.size() > 1 && StringUtils.isNotBlank(sliceName)) {
            // TODO: handle slice names
            // targetValue = targetValues.stream()
            targetValue = targetValues.get(0);
        } else {
            targetValue = targetValues.get(index);
        }
        return targetValue;
    }
}
