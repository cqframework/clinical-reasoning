package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IDomainResource;

public abstract class BaseDomainResourceBuilder<SELF, T extends IDomainResource> extends BaseResourceBuilder<SELF, T> {

    private List<Pair<String, CodeableConceptSettings>> extension;
    private List<Pair<String, CodeableConceptSettings>> modifierExtension;

    protected BaseDomainResourceBuilder(Class<T> resourceClass) {
        super(resourceClass);
    }

    protected BaseDomainResourceBuilder(Class<T> resourceClass, String id) {
        super(resourceClass, id);
    }

    private void addExtension(Pair<String, CodeableConceptSettings> extension) {
        if (this.extension == null) {
            this.extension = new ArrayList<>();
        }
        this.extension.add(extension);
    }

    private List<Pair<String, CodeableConceptSettings>> getExtensions() {
        if (extension == null) {
            return Collections.emptyList();
        }
        return extension;
    }

    private void addModifierExtension(Pair<String, CodeableConceptSettings> modifierExtension) {
        if (this.modifierExtension == null) {
            this.modifierExtension = new ArrayList<>();
        }
        this.modifierExtension.add(modifierExtension);
    }

    private List<Pair<String, CodeableConceptSettings>> getModifierExtensions() {
        if (modifierExtension == null) {
            return Collections.emptyList();
        }
        return modifierExtension;
    }

    public SELF withExtension(Pair<String, CodeableConceptSettings> extension) {
        checkNotNull(extension);

        addExtension(extension);

        return self();
    }

    public SELF withModifierExtension(Pair<String, CodeableConceptSettings> modifierExtension) {
        checkNotNull(modifierExtension);

        addModifierExtension(modifierExtension);

        return self();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SELF self() {
        return (SELF) this;
    }

    @Override
    protected void initializeDstu3(T resource) {
        super.initializeDstu3(resource);

        getExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept =
                            new org.hl7.fhir.dstu3.model.CodeableConcept()
                                    .addCoding(new org.hl7.fhir.dstu3.model.Coding()
                                            .setSystem(coding.getSystem())
                                            .setCode(coding.getCode())
                                            .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> extension = resource.addExtension();
                    extension.setUrl(extensionSetting.getKey());
                    extension.setValue(codeableConcept);
                }));

        getModifierExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept =
                            new org.hl7.fhir.dstu3.model.CodeableConcept()
                                    .addCoding(new org.hl7.fhir.dstu3.model.Coding()
                                            .setSystem(coding.getSystem())
                                            .setCode(coding.getCode())
                                            .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> modifierExtension = resource.addModifierExtension();
                    modifierExtension.setUrl(extensionSetting.getKey());
                    modifierExtension.setValue(codeableConcept);
                }));
    }

    @Override
    protected void initializeR4(T resource) {
        super.initializeR4(resource);

        getExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.r4.model.CodeableConcept codeableConcept = new org.hl7.fhir.r4.model.CodeableConcept()
                            .addCoding(new org.hl7.fhir.r4.model.Coding()
                                    .setSystem(coding.getSystem())
                                    .setCode(coding.getCode())
                                    .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> extension = resource.addExtension();
                    extension.setUrl(extensionSetting.getKey());
                    extension.setValue(codeableConcept);
                }));

        getModifierExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.r4.model.CodeableConcept codeableConcept = new org.hl7.fhir.r4.model.CodeableConcept()
                            .addCoding(new org.hl7.fhir.r4.model.Coding()
                                    .setSystem(coding.getSystem())
                                    .setCode(coding.getCode())
                                    .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> modifierExtension = resource.addModifierExtension();
                    modifierExtension.setUrl(extensionSetting.getKey());
                    modifierExtension.setValue(codeableConcept);
                }));
    }

    @Override
    protected void initializeR5(T resource) {
        super.initializeR5(resource);

        getExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.r5.model.CodeableConcept codeableConcept = new org.hl7.fhir.r5.model.CodeableConcept()
                            .addCoding(new org.hl7.fhir.r5.model.Coding()
                                    .setSystem(coding.getSystem())
                                    .setCode(coding.getCode())
                                    .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> extension = resource.addExtension();
                    extension.setUrl(extensionSetting.getKey());
                    extension.setValue(codeableConcept);
                }));

        getModifierExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.r5.model.CodeableConcept codeableConcept = new org.hl7.fhir.r5.model.CodeableConcept()
                            .addCoding(new org.hl7.fhir.r5.model.Coding()
                                    .setSystem(coding.getSystem())
                                    .setCode(coding.getCode())
                                    .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> modifierExtension = resource.addModifierExtension();
                    modifierExtension.setUrl(extensionSetting.getKey());
                    modifierExtension.setValue(codeableConcept);
                }));
    }
}
