package org.opencds.cqf.fhir.cql.engine.utility;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeCompositeDatatypeDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseEnumFactory;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.runtime.Code;

public class CodeExtractor {
    private final RuntimeCompositeDatatypeDefinition conceptDefinition;
    private final RuntimeCompositeDatatypeDefinition codingDefinition;

    private final BaseRuntimeChildDefinition conceptCodingChild;

    private final BaseRuntimeChildDefinition versionDefinition;
    private final BaseRuntimeChildDefinition codeDefinition;
    private final BaseRuntimeChildDefinition systemDefinition;
    private final BaseRuntimeChildDefinition displayDefinition;

    public CodeExtractor(FhirContext fhirContext) {
        this.conceptDefinition =
                (RuntimeCompositeDatatypeDefinition) fhirContext.getElementDefinition("CodeableConcept");
        this.conceptCodingChild = conceptDefinition.getChildByName("coding");

        this.codingDefinition = (RuntimeCompositeDatatypeDefinition) fhirContext.getElementDefinition("Coding");
        this.versionDefinition = codingDefinition.getChildByName("version");
        this.codeDefinition = codingDefinition.getChildByName("code");
        this.systemDefinition = codingDefinition.getChildByName("system");
        this.displayDefinition = codingDefinition.getChildByName("display");
    }

    public List<Code> getElmCodesFromObject(Object object) {
        var codes = new ArrayList<Code>();
        if (object instanceof Iterable<?> iterable) {
            for (Object innerObject : iterable) {
                List<Code> elmCodes = getElmCodesFromObject(innerObject);
                if (elmCodes != null) {
                    codes.addAll(elmCodes);
                }
            }
        } else {
            var elmCodes = getElmCodesFromObjectInner(object);
            if (elmCodes != null) {
                codes.addAll(elmCodes);
            }
        }
        return codes;
    }

    private List<Code> getElmCodesFromObjectInner(Object object) {
        List<Code> codes = new ArrayList<>();
        if (object == null) {
            return codes;
        } else if (object instanceof IBase base) {
            List<Code> innerCodes = getCodesFromBase(base);
            if (innerCodes != null) {
                codes.addAll(innerCodes);
            }
        } else if (object instanceof Code code) {
            codes.add(code);
        } else {
            throw new IllegalArgumentException("Unable to extract codes from object %s".formatted(object.toString()));
        }

        return codes;
    }

    private List<Code> getCodesFromBase(IBase object) {
        if (object instanceof org.hl7.fhir.instance.model.api.IBaseEnumeration<?>) {
            @SuppressWarnings("unchecked")
            IBaseEnumeration<Enum<?>> enumeration = ((IBaseEnumeration<Enum<?>>) object);
            return this.getCodeFromEnumeration(enumeration);
        } else if (object.fhirType().equals("CodeableConcept")) {
            return this.getCodesInConcept(object);
        } else if (object.fhirType().equals("Coding")) {
            return this.generateCodes(Collections.singletonList(object));
        }

        throw new IllegalArgumentException("Unable to extract codes from fhirType %s".formatted(object.fhirType()));
    }

    private List<Code> getCodeFromEnumeration(IBaseEnumeration<Enum<?>> enumeration) {
        var codes = new ArrayList<Code>();
        if (enumeration == null) {
            return codes;
        }

        IBaseEnumFactory<Enum<?>> enumFactory = enumeration.getEnumFactory();

        String system = enumFactory.toSystem(enumeration.getValue());
        String codeAsString = enumFactory.toCode(enumeration.getValue());
        if (system != null && !system.isEmpty() && codeAsString != null && !codeAsString.isEmpty()) {
            Code code = new Code();
            code.setCode(codeAsString);
            code.setSystem(system);
            codes.add(code);
        }

        return codes;
    }

    private List<Code> getCodesInConcept(IBase object) {
        List<IBase> codingObjects = getCodingObjects(object);
        if (codingObjects == null) {
            return null;
        }
        return generateCodes(codingObjects);
    }

    private List<Code> generateCodes(List<IBase> codingObjects) {

        List<Code> codes = new ArrayList<>();
        for (IBase coding : codingObjects) {
            String code = getStringValueFromPrimitiveDefinition(this.codeDefinition, coding);
            String display = getStringValueFromPrimitiveDefinition(this.displayDefinition, coding);
            String system = getStringValueFromPrimitiveDefinition(this.systemDefinition, coding);
            String version = getStringValueFromPrimitiveDefinition(this.versionDefinition, coding);
            codes.add(new Code()
                    .withSystem(system)
                    .withCode(code)
                    .withDisplay(display)
                    .withVersion(version));
        }
        return codes;
    }

    private List<IBase> getCodingObjects(IBase object) {
        List<IBase> codingObject = null;
        try {
            codingObject = this.conceptCodingChild.getAccessor().getValues(object);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return codingObject;
    }

    private String getStringValueFromPrimitiveDefinition(BaseRuntimeChildDefinition definition, IBase value) {
        IAccessor accessor = definition.getAccessor();
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
}
