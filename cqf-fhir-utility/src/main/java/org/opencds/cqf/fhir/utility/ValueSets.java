package org.opencds.cqf.fhir.utility;

import static org.opencds.cqf.fhir.utility.Resources.newBackboneElement;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition;
import ca.uhn.fhir.context.RuntimeResourceBlockDefinition;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.fhirpath.IFhirPath;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.runtime.Code;

public class ValueSets {

    private ValueSets() {
        // intentionally empty
    }

    public static IBase getCompose(FhirContext fhirContext, IBaseResource valueSet) {
        BaseRuntimeChildDefinition composeChild = getComposeDefinition(fhirContext);
        List<IBase> compose = composeChild.getAccessor().getValues(valueSet);

        if (compose.isEmpty()) {
            return null;
        }

        if (compose.size() > 1) {
            throw new IllegalArgumentException("ValueSet has multiple compose definitions.");
        }

        return compose.get(0);
    }

    public static List<IBase> getIncludes(FhirContext fhirContext, IBaseResource valueSet) {
        IBase compose = getCompose(fhirContext, valueSet);

        if (compose == null) {
            return null;
        }

        BaseRuntimeChildDefinition includeChild = getIncludeDefinition(fhirContext);
        List<IBase> includeComponents = includeChild.getAccessor().getValues(compose);

        if (includeComponents.isEmpty()) {
            return null;
        }

        return includeComponents;
    }

    public static List<IBase> getExcludes(FhirContext fhirContext, IBaseResource valueSet) {
        IBase compose = getCompose(fhirContext, valueSet);

        if (compose == null) {
            return null;
        }

        BaseRuntimeChildDefinition excludeChild = getExcludeDefinition(fhirContext);
        List<IBase> excludeComponents = excludeChild.getAccessor().getValues(compose);

        if (excludeComponents == null || excludeComponents.isEmpty()) {
            return null;
        }

        return excludeComponents;
    }

    public static List<IBase> getIncludeConcepts(FhirContext fhirContext, IBaseResource valueSet) {
        List<IBase> includes = getIncludes(fhirContext, valueSet);

        if (includes == null) {
            return null;
        }

        BaseRuntimeChildDefinition conceptChild = getIncludeConceptDefinition(fhirContext);

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        List<IBase> concepts = new ArrayList<>();
        for (IBase include : includes) {
            List<IBase> currentConcepts = conceptChild.getAccessor().getValues(include);
            if (currentConcepts != null) {
                concepts.addAll(currentConcepts);
            }
        }

        return concepts;
    }

    public static List<IBase> getIncludeFilters(FhirContext fhirContext, IBaseResource valueSet) {
        List<IBase> includes = getIncludes(fhirContext, valueSet);

        if (includes == null) {
            return null;
        }

        BaseRuntimeChildDefinition filterChild = getIncludeFilterDefinition(fhirContext);

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        List<IBase> filters = new ArrayList<>();
        for (IBase include : includes) {
            List<IBase> currentFilters = filterChild.getAccessor().getValues(include);
            if (currentFilters != null) {
                filters.addAll(currentFilters);
            }
        }

        return filters;
    }

    public static List<IBase> getExcludeConcepts(FhirContext fhirContext, IBaseResource valueSet) {
        List<IBase> excludes = getExcludes(fhirContext, valueSet);

        if (excludes == null) {
            return null;
        }

        BaseRuntimeChildDefinition conceptChild = getExcludeConceptDefinition(fhirContext);

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        List<IBase> concepts = new ArrayList<>();
        for (IBase exclude : excludes) {
            List<IBase> currentConcepts = conceptChild.getAccessor().getValues(exclude);
            if (currentConcepts != null) {
                concepts.addAll(currentConcepts);
            }
        }

        return concepts;
    }

    public static List<IBase> getExcludeFilters(FhirContext fhirContext, IBaseResource valueSet) {
        List<IBase> excludes = getExcludes(fhirContext, valueSet);

        if (excludes == null) {
            return null;
        }

        BaseRuntimeChildDefinition filterChild = getExcludeFilterDefinition(fhirContext);

        // TODO: The system is defined at the include level, while codes are at the concept level
        // Need to return a class that represents that.
        List<IBase> filters = new ArrayList<>();
        for (IBase exclude : excludes) {
            List<IBase> currentFilters = filterChild.getAccessor().getValues(exclude);
            if (currentFilters != null) {
                filters.addAll(currentFilters);
            }
        }

        return filters;
    }

    public static IBase getExpansion(FhirContext fhirContext, IBaseResource valueSet) {
        BaseRuntimeChildDefinition expansionChild = getExpansionDefinition(fhirContext);
        List<IBase> expansion = expansionChild.getAccessor().getValues(valueSet);

        if (expansion == null || expansion.isEmpty()) {
            return null;
        }

        if (expansion.size() > 1) {
            throw new IllegalArgumentException("ValueSet has multiple expansion definitions.");
        }

        return expansion.get(0);
    }

    public static List<IBase> getContainsInExpansion(FhirContext fhirContext, IBase expansion) {
        if (expansion == null) {
            return null;
        }

        BaseRuntimeChildDefinition containsDefinition = getContainsDefinition(fhirContext);

        List<IBase> contains = containsDefinition.getAccessor().getValues(expansion);

        if (contains == null || contains.isEmpty()) {
            return null;
        }

        return contains;
    }

    public static List<IBase> getContains(FhirContext fhirContext, IBaseResource valueSet) {
        return getContainsInExpansion(fhirContext, getExpansion(fhirContext, valueSet));
    }

    public static List<Code> getCodesInCompose(FhirContext fhirContext, IBaseResource valueSet) {
        List<IBase> includes = getIncludes(fhirContext, valueSet);

        if (includes == null) {
            return null;
        }

        BaseRuntimeChildDefinition conceptChild = getIncludeConceptDefinition(fhirContext);

        IAccessor versionAccessor = getIncludeVersionDefinition(fhirContext).getAccessor();
        IAccessor systemAccessor = getIncludeSystemDefinition(fhirContext).getAccessor();
        IAccessor codeAccessor = getIncludeConceptCodeDefinition(fhirContext).getAccessor();
        IAccessor displayAccessor =
                getIncludeConceptDisplayDefinition(fhirContext).getAccessor();

        List<Code> codes = new ArrayList<>();
        for (IBase include : includes) {

            String version = getStringValueFromPrimitiveAccessor(include, versionAccessor);
            String system = getStringValueFromPrimitiveAccessor(include, systemAccessor);

            List<IBase> concepts = conceptChild.getAccessor().getValues(include);

            for (IBase c : concepts) {

                String code = getStringValueFromPrimitiveAccessor(c, codeAccessor);
                String display = getStringValueFromPrimitiveAccessor(c, displayAccessor);
                codes.add(new Code()
                        .withSystem(system)
                        .withCode(code)
                        .withDisplay(display)
                        .withVersion(version));
            }
        }

        return codes;
    }

    public static List<Code> getCodesInContains(FhirContext fhirContext, List<IBase> contains) {
        if (contains == null) {
            return null;
        }

        IAccessor systemAccessor = getSystemDefinition(fhirContext).getAccessor();
        IAccessor codeAccessor = getCodeDefinition(fhirContext).getAccessor();
        IAccessor displayAccessor = getDisplayDefinition(fhirContext).getAccessor();
        IAccessor versionAccessor = getVersionDefinition(fhirContext).getAccessor();

        List<Code> codes = new ArrayList<>();
        for (IBase c : contains) {

            String system = getStringValueFromPrimitiveAccessor(c, systemAccessor);
            String code = getStringValueFromPrimitiveAccessor(c, codeAccessor);
            String display = getStringValueFromPrimitiveAccessor(c, displayAccessor);
            String version = getStringValueFromPrimitiveAccessor(c, versionAccessor);

            codes.add(new Code()
                    .withSystem(system)
                    .withCode(code)
                    .withDisplay(display)
                    .withVersion(version));
        }

        return codes;
    }

    public static List<Code> getCodesInExpansion(FhirContext fhirContext, IBase expansion) {
        return getCodesInContains(fhirContext, getContainsInExpansion(fhirContext, expansion));
    }

    public static List<Code> getCodesInExpansion(FhirContext fhirContext, IBaseResource valueSet) {
        return getCodesInContains(fhirContext, getContains(fhirContext, valueSet));
    }

    @SuppressWarnings("unchecked")
    public static void addCodeToExpansion(FhirContext fhirContext, IBase expansion, Code code)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    NoSuchMethodException, SecurityException {
        var containsDef = getContainsDefinition(fhirContext);
        var systemDef = getSystemDefinition(fhirContext);
        var codeDef = getCodeDefinition(fhirContext);
        var displayDef = getDisplayDefinition(fhirContext);
        var versionDef = getVersionDefinition(fhirContext);
        var newCode = newBackboneElement((Class<? extends IBaseBackboneElement>)
                containsDef.getChildByName("contains").getImplementingClass());
        systemDef
                .getMutator()
                .addValue(
                        newCode,
                        systemDef
                                .getChildByName("system")
                                .getImplementingClass()
                                .getConstructor(String.class)
                                .newInstance(code.getSystem()));
        codeDef.getMutator()
                .addValue(
                        newCode,
                        codeDef.getChildByName("code")
                                .getImplementingClass()
                                .getConstructor(String.class)
                                .newInstance(code.getCode()));
        displayDef
                .getMutator()
                .addValue(
                        newCode,
                        displayDef
                                .getChildByName("display")
                                .getImplementingClass()
                                .getConstructor(String.class)
                                .newInstance(code.getDisplay()));
        versionDef
                .getMutator()
                .addValue(
                        newCode,
                        versionDef
                                .getChildByName("version")
                                .getImplementingClass()
                                .getConstructor(String.class)
                                .newInstance(code.getVersion()));
        containsDef.getMutator().addValue(expansion, newCode);
    }

    public static void addParameterToExpansion(
            FhirContext fhirContext, IBase expansion, IBaseBackboneElement parameter) {
        getParameterDefinition(fhirContext).getMutator().addValue(expansion, parameter);
    }

    public static String getUrl(FhirContext fhirContext, IBaseResource valueSet) {
        BaseRuntimeChildDefinition urlDef = getUrlDefinition(fhirContext);
        return getStringValueFromPrimitiveAccessor(valueSet, urlDef.getAccessor());
    }

    public static String getId(FhirContext fhirContext, IBaseResource valueSet) {
        BaseRuntimeChildDefinition idDef = getIdDefinition(fhirContext);
        return getStringValueFromPrimitiveAccessor(valueSet, idDef.getAccessor());
    }

    private static String getStringValueFromPrimitiveAccessor(IBase value, IAccessor accessor) {
        if (value == null || accessor == null) {
            return null;
        }

        List<IBase> values = accessor.getValues(value);
        if (values == null || values.isEmpty()) {
            return null;
        }

        if (values.size() > 1) {
            throw new IllegalArgumentException(
                    "More than one value returned while attempting to access primitive value.");
        }

        IBase baseValue = values.get(0);

        if (!(baseValue instanceof IPrimitiveType)) {
            throw new IllegalArgumentException(
                    "Non-primitive value encountered while trying to access primitive value.");
        } else {
            return ((IPrimitiveType<?>) baseValue).getValueAsString();
        }
    }

    private static BaseRuntimeChildDefinition getComposeDefinition(FhirContext fhirContext) {
        RuntimeResourceDefinition def = fhirContext.getResourceDefinition("ValueSet");
        return def.getChildByName("compose");
    }

    private static BaseRuntimeChildDefinition getIncludeDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition composeChild = getComposeDefinition(fhirContext);
        return getIncludeDefinition(composeChild);
    }

    private static BaseRuntimeChildDefinition getIncludeDefinition(BaseRuntimeChildDefinition composeChild) {
        RuntimeResourceBlockDefinition composeBlockChild =
                (RuntimeResourceBlockDefinition) composeChild.getChildByName("compose");
        BaseRuntimeChildDefinition includeChild = composeBlockChild.getChildByName("include");
        return includeChild;
    }

    private static BaseRuntimeChildDefinition getIncludeConceptDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition includeChild = getIncludeDefinition(fhirContext);
        RuntimeResourceBlockDefinition includeBlockChild =
                (RuntimeResourceBlockDefinition) includeChild.getChildByName("include");
        return getConceptDefinition(includeBlockChild);
    }

    private static BaseRuntimeChildDefinition getIncludeFilterDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition includeChild = getIncludeDefinition(fhirContext);
        RuntimeResourceBlockDefinition includeBlockChild =
                (RuntimeResourceBlockDefinition) includeChild.getChildByName("include");
        return getFilterDefinition(includeBlockChild);
    }

    private static RuntimeChildResourceBlockDefinition getConceptDefinition(
            RuntimeResourceBlockDefinition includeOrExcludeChild) {
        return (RuntimeChildResourceBlockDefinition) includeOrExcludeChild.getChildByName("concept");
    }

    private static RuntimeChildResourceBlockDefinition getFilterDefinition(
            RuntimeResourceBlockDefinition includeOrExcludeChild) {
        return (RuntimeChildResourceBlockDefinition) includeOrExcludeChild.getChildByName("filter");
    }

    private static BaseRuntimeChildDefinition getExcludeDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition composeChild = getComposeDefinition(fhirContext);
        return getExcludeDefinition(composeChild);
    }

    private static BaseRuntimeChildDefinition getExcludeDefinition(BaseRuntimeChildDefinition composeChild) {
        RuntimeResourceBlockDefinition composeBlockChild =
                (RuntimeResourceBlockDefinition) composeChild.getChildByName("compose");
        BaseRuntimeChildDefinition excludeChild = composeBlockChild.getChildByName("exclude");
        return excludeChild;
    }

    private static BaseRuntimeChildDefinition getExcludeConceptDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition excludeChild = getExcludeDefinition(fhirContext);
        RuntimeResourceBlockDefinition excludeBlockChild =
                (RuntimeResourceBlockDefinition) excludeChild.getChildByName("exclude");
        return getConceptDefinition(excludeBlockChild);
    }

    private static BaseRuntimeChildDefinition getExcludeFilterDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition excludeChild = getExcludeDefinition(fhirContext);
        RuntimeResourceBlockDefinition excludeBlockChild =
                (RuntimeResourceBlockDefinition) excludeChild.getChildByName("exclude");
        return getFilterDefinition(excludeBlockChild);
    }

    private static BaseRuntimeChildDefinition getExpansionDefinition(FhirContext fhirContext) {
        RuntimeResourceDefinition def = fhirContext.getResourceDefinition("ValueSet");
        return def.getChildByName("expansion");
    }

    private static BaseRuntimeChildDefinition getParameterDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition expansionChild = getExpansionDefinition(fhirContext);
        RuntimeResourceBlockDefinition expansionBlockChild =
                (RuntimeResourceBlockDefinition) expansionChild.getChildByName("expansion");
        return getParameterDefinition(expansionBlockChild);
    }

    private static BaseRuntimeChildDefinition getParameterDefinition(RuntimeResourceBlockDefinition expansionChild) {
        return expansionChild.getChildByName("parameter");
    }

    private static BaseRuntimeChildDefinition getContainsDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition expansionChild = getExpansionDefinition(fhirContext);
        RuntimeResourceBlockDefinition expansionBlockChild =
                (RuntimeResourceBlockDefinition) expansionChild.getChildByName("expansion");
        return getContainsDefinition(expansionBlockChild);
    }

    private static BaseRuntimeChildDefinition getContainsDefinition(RuntimeResourceBlockDefinition expansionChild) {
        return expansionChild.getChildByName("contains");
    }

    private static BaseRuntimeChildDefinition getSystemDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition containsDefinition = getContainsDefinition(fhirContext);
        RuntimeResourceBlockDefinition containsBlockDefinition =
                (RuntimeResourceBlockDefinition) containsDefinition.getChildByName("contains");
        return containsBlockDefinition.getChildByName("system");
    }

    private static BaseRuntimeChildDefinition getVersionDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition containsDefinition = getContainsDefinition(fhirContext);
        RuntimeResourceBlockDefinition containsBlockDefinition =
                (RuntimeResourceBlockDefinition) containsDefinition.getChildByName("contains");
        return containsBlockDefinition.getChildByName("version");
    }

    private static BaseRuntimeChildDefinition getCodeDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition containsDefinition = getContainsDefinition(fhirContext);
        RuntimeResourceBlockDefinition containsBlockDefinition =
                (RuntimeResourceBlockDefinition) containsDefinition.getChildByName("contains");
        return containsBlockDefinition.getChildByName("code");
    }

    private static BaseRuntimeChildDefinition getDisplayDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition containsDefinition = getContainsDefinition(fhirContext);
        RuntimeResourceBlockDefinition containsBlockDefinition =
                (RuntimeResourceBlockDefinition) containsDefinition.getChildByName("contains");
        return containsBlockDefinition.getChildByName("display");
    }

    private static BaseRuntimeChildDefinition getIncludeConceptCodeDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition includeConceptDefinition = getIncludeConceptDefinition(fhirContext);
        RuntimeResourceBlockDefinition containsBlockDefinition =
                (RuntimeResourceBlockDefinition) includeConceptDefinition.getChildByName("concept");
        return containsBlockDefinition.getChildByName("code");
    }

    private static BaseRuntimeChildDefinition getIncludeConceptDisplayDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition includeConceptDefinition = getIncludeConceptDefinition(fhirContext);
        RuntimeResourceBlockDefinition containsBlockDefinition =
                (RuntimeResourceBlockDefinition) includeConceptDefinition.getChildByName("concept");
        return containsBlockDefinition.getChildByName("display");
    }

    private static BaseRuntimeChildDefinition getIncludeSystemDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition includeDefinition = getIncludeDefinition(fhirContext);
        RuntimeResourceBlockDefinition includeBlockDefinition =
                (RuntimeResourceBlockDefinition) includeDefinition.getChildByName("include");
        return includeBlockDefinition.getChildByName("system");
    }

    private static BaseRuntimeChildDefinition getIncludeVersionDefinition(FhirContext fhirContext) {
        BaseRuntimeChildDefinition includeDefinition = getIncludeDefinition(fhirContext);
        RuntimeResourceBlockDefinition includeBlockDefinition =
                (RuntimeResourceBlockDefinition) includeDefinition.getChildByName("include");
        return includeBlockDefinition.getChildByName("version");
    }

    private static BaseRuntimeChildDefinition getUrlDefinition(FhirContext fhirContext) {
        RuntimeResourceDefinition def = fhirContext.getResourceDefinition("ValueSet");
        return def.getChildByName("url");
    }

    private static BaseRuntimeChildDefinition getIdDefinition(FhirContext fhirContext) {
        RuntimeResourceDefinition def = fhirContext.getResourceDefinition("ValueSet");
        return def.getChildByName("id");
    }

    public static <T extends IBase> List<IBase> getExpansionParameters(
            IBase expansion, IFhirPath fhirPath, String filterExpression) {
        // String expression = "expansion.parameter";
        // if (filterExpression != null) { expression = expression + filterExpression; }
        String expression = String.format("parameter%s", filterExpression);
        // String expression = (filterExpression == null) ? "expansion.parameter" :
        // "expansion.parameter" + filterExpression;
        return fhirPath.evaluate(expansion, expression, IBase.class);
    }
}
