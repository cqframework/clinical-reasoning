package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseDatatypeElement;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class ElementDefinitionAdapter implements IElementDefinitionAdapter {

    private final ElementDefinition elementDefinition;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;
    private final AdapterFactory adapterFactory;

    public ElementDefinitionAdapter(ICompositeType elementDefinition) {
        if (!(elementDefinition instanceof ElementDefinition)) {
            throw new IllegalArgumentException(
                    "object passed as elementDefinition argument is not a ElementDefinition data type");
        }
        this.elementDefinition = (ElementDefinition) elementDefinition;
        fhirContext = FhirContext.forDstu3Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R5);
        adapterFactory = new AdapterFactory();
    }

    @Override
    public ElementDefinition get() {
        return elementDefinition;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    @Override
    public String getId() {
        return get().getId();
    }

    @Override
    public String getPath() {
        return get().getPath();
    }

    @Override
    public String getSliceName() {
        return get().getSliceName();
    }

    @Override
    public boolean hasSlicing() {
        return get().hasSlicing();
    }

    @Override
    public String getLabel() {
        return get().getLabel();
    }

    @Override
    public boolean hasLabel() {
        return get().hasLabel();
    }

    @Override
    public List<ICodingAdapter> getCode() {
        return get().getCode().stream().map(adapterFactory::createCoding).collect(Collectors.toList());
    }

    @Override
    public String getShort() {
        return get().getShort();
    }

    @Override
    public boolean hasShort() {
        return get().hasShort();
    }

    @Override
    public String getDefinition() {
        return get().getDefinition();
    }

    @Override
    public String getComment() {
        return get().getComment();
    }

    @Override
    public String getRequirements() {
        return get().getRequirements();
    }

    @Override
    public List<String> getAlias() {
        return get().getAlias().stream().map(a -> a.asStringValue()).collect(Collectors.toList());
    }

    @Override
    public int getMin() {
        return get().getMin();
    }

    @Override
    public boolean hasMin() {
        return get().hasMin();
    }

    @Override
    public String getMax() {
        return get().getMax();
    }

    @Override
    public boolean hasMax() {
        return get().hasMax();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IBase> List<T> getType() {
        return get().getType().stream().map(t -> (T) t).collect(Collectors.toList());
    }

    @Override
    public String getTypeCode() {
        return get().getTypeFirstRep().getCode();
    }

    @Override
    public String getTypeProfile() {
        return get().getTypeFirstRep().getProfile();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IBaseDatatype> T getDefaultValue() {
        return (T) get().getDefaultValue();
    }

    @Override
    public boolean hasDefaultValue() {
        return get().hasDefaultValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IBaseDatatype> T getFixed() {
        return (T) get().getFixed();
    }

    @Override
    public boolean hasFixed() {
        return get().hasFixed();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IBaseDatatype> T getPattern() {
        return (T) get().getPattern();
    }

    @Override
    public boolean hasPattern() {
        return get().hasPattern();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IBaseDatatype> T getFixedOrPattern() {
        return (T) (hasFixed() ? get().getFixed() : get().getPattern());
    }

    @Override
    public boolean hasFixedOrPattern() {
        return hasFixed() || hasPattern();
    }

    @Override
    public <T extends IBaseDatatype> T getDefaultOrFixedOrPattern() {
        return hasFixedOrPattern() ? getFixedOrPattern() : getDefaultValue();
    }

    @Override
    public boolean hasDefaultOrFixedOrPattern() {
        return hasDefaultValue() || hasFixedOrPattern();
    }

    @Override
    public boolean getMustSupport() {
        return get().getMustSupport();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IBaseDatatypeElement> T getBinding() {
        return (T) get().getBinding();
    }

    @Override
    public boolean hasBinding() {
        return get().hasBinding();
    }

    @Override
    public String getBindingValueSet() {
        if (hasBinding()) {
            var valueSet = get().getBinding().getValueSet();
            return valueSet instanceof Reference reference ? reference.getReference() : valueSet.primitiveValue();
        }
        return null;
    }
}
