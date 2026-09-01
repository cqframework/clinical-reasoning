package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cql.ClassInstanceHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;

/**
 * This is some hackery because most of these objects don't implement
 * hashCode or equals, meaning it's hard to detect distinct values;
 */
public class StratumValueWrapper {

    protected Object value;

    /**
     * {@link #getKey()} is called once per element by {@code hashCode()} and twice per comparison by
     * {@code equals()}, so accumulating a frequency map walks the rendering chain below several times
     * for every value. The wrapped value does not change after construction, so the key does not either.
     */
    private String cachedKey;

    public StratumValueWrapper(Object value) {
        this.value = normalizeEngineNativeValue(value);
    }

    /**
     * CQL-5 stratifier/SDE results arrive as engine-native values: FHIR resources and complex types
     * as {@link org.opencds.cqf.cql.engine.runtime.ClassInstance}, and primitives as CQL
     * {@link org.opencds.cqf.cql.engine.runtime.SimpleValue}s (e.g. CQL String, Integer). Their
     * {@code toString()} adds CQL formatting (a CQL String renders as {@code 'male'}, not
     * {@code male}), so left unnormalized they fall through the typed-rendering branches below to
     * {@code toString()} and produce wrong stratum values/keys. Normalize once at construction so
     * the FHIR-typed and primitive rendering branches apply.
     */
    private static Object normalizeEngineNativeValue(Object rawValue) {
        // A FHIR *resource* is rendered here by its id alone, and the ClassInstance already carries it.
        // Converting one first means reflectively rebuilding its whole element graph - for an
        // ExplanationOfBenefit, dozens of nested backbone elements - to read a single field off it and
        // discard the rest, once per occurrence per subject. Leave resources engine-native; the report
        // builders convert the ones they actually render.
        if (rawValue instanceof ClassInstance classInstance && ClassInstanceHelper.isFhirResource(classInstance)) {
            return rawValue;
        }
        // Complex datatypes (Coding, CodeableConcept, Identifier) still convert: the rendering below
        // reads their contents through IAdapterFactory, and they are a handful of primitives, not a graph.
        var converted = ClassInstanceHelper.convertToFhirR4IfNeeded(rawValue);
        if (converted != rawValue) {
            return converted;
        }
        // CQL SimpleValue primitive -> underlying plain Java value
        if (rawValue instanceof org.opencds.cqf.cql.engine.runtime.String cqlString) {
            return cqlString.getValue();
        }
        if (rawValue instanceof org.opencds.cqf.cql.engine.runtime.Boolean cqlBoolean) {
            return cqlBoolean.getValue();
        }
        if (rawValue instanceof org.opencds.cqf.cql.engine.runtime.Integer cqlInteger) {
            return cqlInteger.getValue();
        }
        if (rawValue instanceof org.opencds.cqf.cql.engine.runtime.Long cqlLong) {
            return cqlLong.getValue();
        }
        if (rawValue instanceof org.opencds.cqf.cql.engine.runtime.Decimal cqlDecimal) {
            return cqlDecimal.getValue();
        }
        return rawValue;
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }

        StratumValueWrapper other = (StratumValueWrapper) o;

        if (other.getValue() == null ^ this.getValue() == null) {
            return false;
        }

        if (other.getValue() == null && this.getValue() == null) {
            return true;
        }

        return this.getKey().equals(other.getKey());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StratumValueWrapper.class.getSimpleName() + "[", "]")
                .add("value=" + value)
                .toString();
    }

    /**
     * Sentinel value used for null stratum values.
     * This allows subjects with null stratifier results to be grouped into their own stratum.
     */
    private static final String NULL_STRATUM_VALUE = "null";

    /**
     * Sentinel value used for empty collection stratum values.
     * This allows subjects with empty list/collection stratifier results to be grouped into their own stratum.
     */
    private static final String EMPTY_STRATUM_VALUE = "empty";

    public String getKey() {
        if (cachedKey == null) {
            cachedKey = computeKey();
        }
        return cachedKey;
    }

    private String computeKey() {
        var wrapper = CqlExpressionValue.ofRaw(null, value, null);
        // Handle null values - group them into a special "null" stratum
        if (wrapper.isNull()) {
            return NULL_STRATUM_VALUE;
        }

        // Handle empty collections - group them into a special "empty" stratum
        if (wrapper.isEmpty()) {
            return EMPTY_STRATUM_VALUE;
        }

        String key = null;
        var engineNativeResourceId = engineNativeResourceId(value);
        if (value instanceof IBaseCoding) {
            // ASSUMPTION: We won't have different systems with the same code
            // within a given stratifier / sde
            ICodingAdapter coding = createCodingAdapter(value);
            key = joinValues("coding", coding.getCode());
        } else if (isCodeableConcept(value)) {
            ICodingAdapter coding = createCodeableConceptAdapter(value);
            key = joinValues("codeable-concept", coding.getCode());
        } else if (value instanceof Code c) {
            key = joinValues("code", c.getCode());
        } else if (value instanceof Enum<?> e) {
            key = joinValues("enum", e.toString());
        } else if (value instanceof IPrimitiveType<?> p) {
            key = joinValues("primitive", p.getValueAsString());
        } else if (isIdentifier(value)) {
            key = adapterFactoryFor((IBase) value)
                    .createIdentifier((IBase) value)
                    .getValue();
        } else if (engineNativeResourceId != null) {
            key = engineNativeResourceId;
        } else if (value instanceof IBaseResource resource) {
            key = resource.getIdElement().toVersionless().getValue();
        } else {
            key = value.toString();
        }

        if (key == null) {
            throw new InvalidRequestException("found a null key for the wrapped value: %s".formatted(value));
        }

        return key;
    }

    public String getValueAsString() {
        return getValueAsString(this.value);
    }

    public String getDescription() {
        var wrapper = CqlExpressionValue.ofRaw(null, value, null);
        if (wrapper.isNull()) {
            return NULL_STRATUM_VALUE;
        }
        if (wrapper.isEmpty()) {
            return EMPTY_STRATUM_VALUE;
        }
        var engineNativeResourceId = engineNativeResourceId(value);
        if (value instanceof IBaseCoding) {
            ICodingAdapter coding = createCodingAdapter(value);
            return coding.hasDisplay() ? coding.getDisplay() : coding.getCode();
        } else if (isCodeableConcept(value)) {
            ICodingAdapter coding = createCodeableConceptAdapter(value);
            return coding.hasDisplay() ? coding.getDisplay() : coding.getCode();
        } else if (value instanceof Code c) {
            return c.getDisplay() != null ? c.getDisplay() : c.getCode();
        } else if (value instanceof Enum<?> e) {
            return e.toString();
        } else if (value instanceof IPrimitiveType<?> p) {
            return p.getValueAsString();
        } else if (isIdentifier(value)) {
            return adapterFactoryFor((IBase) value)
                    .createIdentifier((IBase) value)
                    .getValue();
        } else if (engineNativeResourceId != null) {
            return engineNativeResourceId;
        } else if (value instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValue();
        } else {
            return value.toString();
        }
    }

    public Object getValue() {
        return this.value;
    }

    public Class<?> getValueClass() {
        if (this.value == null) {
            return String.class;
        }

        return this.value.getClass();
    }

    private String joinValues(String... elements) {
        return String.join("-", elements);
    }

    private String getValueAsString(Object valueInner) {
        // Normalize engine-native values (CQL String -> plain String, FHIR ClassInstance -> HAPI type).
        // The top-level value is normalized at construction, but list elements are not, so normalize
        // here too: a list element left as a CQL String would otherwise fall through to toString() and
        // render with CQL quotes (e.g. 'MMO' instead of MMO).
        valueInner = normalizeEngineNativeValue(valueInner);
        var wrapper = CqlExpressionValue.ofRaw(null, valueInner, null);
        if (wrapper.isNull()) {
            return NULL_STRATUM_VALUE;
        }
        // CQL's empty list "{}" should be a distinct stratum value, not an error
        if (wrapper.isEmpty()) {
            return EMPTY_STRATUM_VALUE;
        }
        var engineNativeResourceId = engineNativeResourceId(valueInner);
        if (valueInner instanceof IBaseCoding) {
            return createCodingAdapter(valueInner).getCode();
        } else if (isCodeableConcept(valueInner)) {
            return createCodeableConceptAdapter(valueInner).getCode();
        } else if (valueInner instanceof Code c) {
            return c.getCode();
        } else if (valueInner instanceof Enum<?> e) {
            return e.toString();
        } else if (valueInner instanceof IPrimitiveType<?> p) {
            return p.getValueAsString();
        } else if (isIdentifier(valueInner)) {
            return adapterFactoryFor((IBase) valueInner)
                    .createIdentifier((IBase) valueInner)
                    .getValue();
        } else if (engineNativeResourceId != null) {
            return engineNativeResourceId;
        } else if (valueInner instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValue();
        } else if (valueInner instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(this::getValueAsString)
                    .limit(5) // stop a massively long string if we have a huge list
                    .collect(Collectors.joining(","));
        } else {
            return valueInner.toString();
        }
    }

    /**
     * The id of a FHIR resource still in the CQL engine's native form, or null for anything else.
     * <p>
     * This is the bare id part, which is what a resource converted from the same {@link ClassInstance}
     * reports from {@code getIdElement()} - the conversion copies {@code id.value} and nothing else, so
     * a value rendered here must agree with it or the same resource would land in two strata depending
     * on whether it happened to be converted. A resource with no id yields null and falls through to
     * the caller's default rendering.
     */
    private static String engineNativeResourceId(Object value) {
        if (value instanceof ClassInstance classInstance && ClassInstanceHelper.isFhirResource(classInstance)) {
            return ClassInstanceHelper.getIdPart(classInstance);
        }
        return null;
    }

    private static boolean isCodeableConcept(Object value) {
        return value instanceof IBase base && "CodeableConcept".equals(base.fhirType());
    }

    private static boolean isIdentifier(Object value) {
        return value instanceof IBase base && "Identifier".equals(base.fhirType());
    }

    private static ICodingAdapter createCodingAdapter(Object value) {
        IBase base = (IBase) value;
        return adapterFactoryFor(base).createCoding(base);
    }

    private static ICodingAdapter createCodeableConceptAdapter(Object value) {
        IBase base = (IBase) value;
        return adapterFactoryFor(base).createCodeableConcept(base).getCodingFirstRep();
    }

    private static IAdapterFactory adapterFactoryFor(IBase base) {
        if (base instanceof IBaseResource resource) {
            return IAdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum());
        }
        String pkg = base.getClass().getPackageName();
        if (pkg.contains(".dstu3.")) {
            return IAdapterFactory.forFhirVersion(FhirVersionEnum.DSTU3);
        }
        if (pkg.contains(".r4.")) {
            return IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
        }
        if (pkg.contains(".r5.")) {
            return IAdapterFactory.forFhirVersion(FhirVersionEnum.R5);
        }
        throw new IllegalArgumentException("Cannot determine FHIR version from: " + base.getClass());
    }
}
