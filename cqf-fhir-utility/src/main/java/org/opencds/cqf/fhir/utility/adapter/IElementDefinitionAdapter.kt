package org.opencds.cqf.fhir.utility.adapter;

import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseDatatypeElement;

public interface IElementDefinitionAdapter extends IAdapter<IBase> {
    String getId();

    String getPath();

    String getSliceName();

    boolean hasSlicing();

    String getLabel();

    boolean hasLabel();

    List<ICodingAdapter> getCode();

    String getShort();

    boolean hasShort();

    String getDefinition();

    String getComment();

    String getRequirements();

    List<String> getAlias();

    int getMin();

    boolean hasMin();

    default boolean isRequired() {
        return getMin() != 0;
    }

    String getMax();

    boolean hasMax();

    <T extends IBase> List<T> getType();

    /**
     * Returns the code of the first rep of the type property.
     * @return
     */
    String getTypeCode();

    /**
     * Returns the first profile of the first rep of the type property.
     * @return
     */
    String getTypeProfile();

    <T extends IBaseDatatype> T getDefaultValue();

    boolean hasDefaultValue();

    <T extends IBaseDatatype> T getFixed();

    boolean hasFixed();

    <T extends IBaseDatatype> T getPattern();

    boolean hasPattern();

    <T extends IBaseDatatype> T getFixedOrPattern();

    boolean hasFixedOrPattern();

    <T extends IBaseDatatype> T getDefaultOrFixedOrPattern();

    boolean hasDefaultOrFixedOrPattern();

    boolean getMustSupport();

    <T extends IBaseDatatypeElement> T getBinding();

    boolean hasBinding();

    String getBindingValueSet();

    boolean isModifier();

    boolean hasCondition();

    int getBaseMin();

    String getBaseMax();

    String getBasePath();

    String getBindingStrength();

    boolean hasMaxLength();

    default List<String> getExtensionUrls() {
        return Collections.emptyList();
    }

    /**
     * Returns true if this element has R5-specific key constraints
     * (mustHaveValue, valueAlternatives, minValue, maxValue).
     * Default returns false for non-R5 versions.
     */
    default boolean hasR5KeyConstraints() {
        return false;
    }
}
