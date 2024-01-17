package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.utility.FhirVersions;
import org.opencds.cqf.fhir.utility.Resources;

public abstract class BaseBackboneElementBuilder<
        SELF extends BaseBackboneElementBuilder<SELF, T>, T extends IBaseBackboneElement> {

    private final Class<T> resourceClass;

    private String id = UUID.randomUUID().toString();

    private List<Pair<String, CodeableConceptSettings>> extensions;
    private List<Pair<String, CodeableConceptSettings>> modifierExtensions;

    protected BaseBackboneElementBuilder(Class<T> resourceClass) {
        checkNotNull(resourceClass);
        this.resourceClass = resourceClass;
    }

    protected BaseBackboneElementBuilder(Class<T> resourceClass, String id) {
        this(resourceClass);
        checkNotNull(id);

        this.id = id;
    }

    public T build() {
        T backboneElement = Resources.newBackboneElement(resourceClass);

        switch (FhirVersions.forClass(resourceClass)) {
            case DSTU3:
                initializeDstu3(backboneElement);
                break;
            case R4:
                initializeR4(backboneElement);
                break;
            case R5:
                initializeR5(backboneElement);
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "ResourceBuilder.initializeResource does not support FHIR version %s",
                        FhirVersions.forClass(resourceClass).getFhirVersionString()));
        }

        return backboneElement;
    }

    private void addExtension(Pair<String, CodeableConceptSettings> extension) {
        if (this.extensions == null) {
            this.extensions = new ArrayList<>();
        }
        this.extensions.add(extension);
    }

    private List<Pair<String, CodeableConceptSettings>> getExtensions() {
        if (extensions == null) {
            return Collections.emptyList();
        }
        return extensions;
    }

    protected String getId() {
        return id;
    }

    private void addModifierExtension(Pair<String, CodeableConceptSettings> modifierExtension) {
        if (this.modifierExtensions == null) {
            this.modifierExtensions = new ArrayList<>();
        }
        this.modifierExtensions.add(modifierExtension);
    }

    private List<Pair<String, CodeableConceptSettings>> getModifierExtensions() {
        if (modifierExtensions == null) {
            return Collections.emptyList();
        }
        return modifierExtensions;
    }

    public SELF withId(String id) {
        checkNotNull(id);

        this.id = id;

        return self();
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
    protected SELF self() {
        return (SELF) this;
    }

    protected void initializeDstu3(T resource) {
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

    protected void initializeR4(T resource) {
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

    protected void initializeR5(T resource) {
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
