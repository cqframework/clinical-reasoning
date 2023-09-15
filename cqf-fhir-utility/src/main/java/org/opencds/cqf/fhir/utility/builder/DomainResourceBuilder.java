package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IDomainResource;

public abstract class DomainResourceBuilder<SELF, T extends IDomainResource> extends ResourceBuilder<SELF, T> {

    private List<Pair<String, CodeableConceptSettings>> myExtension;
    private List<Pair<String, CodeableConceptSettings>> myModifierExtension;

    protected DomainResourceBuilder(Class<T> theResourceClass) {
        super(theResourceClass);
    }

    protected DomainResourceBuilder(Class<T> theResourceClass, String theId) {
        super(theResourceClass, theId);
    }

    private void addExtension(Pair<String, CodeableConceptSettings> theExtension) {
        if (myExtension == null) {
            myExtension = new ArrayList<>();
        }
        myExtension.add(theExtension);
    }

    private List<Pair<String, CodeableConceptSettings>> getExtensions() {
        if (myExtension == null) {
            return Collections.emptyList();
        }
        return myExtension;
    }

    private void addModifierExtension(Pair<String, CodeableConceptSettings> theModifierExtension) {
        if (myModifierExtension == null) {
            myModifierExtension = new ArrayList<>();
        }
        myModifierExtension.add(theModifierExtension);
    }

    private List<Pair<String, CodeableConceptSettings>> getModifierExtensions() {
        if (myModifierExtension == null) {
            return Collections.emptyList();
        }
        return myModifierExtension;
    }

    public SELF withExtension(Pair<String, CodeableConceptSettings> theExtension) {
        checkNotNull(theExtension);

        addExtension(theExtension);

        return self();
    }

    public SELF withModifierExtension(Pair<String, CodeableConceptSettings> theModifierExtension) {
        checkNotNull(theModifierExtension);

        addModifierExtension(theModifierExtension);

        return self();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SELF self() {
        return (SELF) this;
    }

    @Override
    protected void initializeDstu3(T theResource) {
        super.initializeDstu3(theResource);

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
                    IBaseExtension<?, ?> extension = theResource.addExtension();
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
                    IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
                    modifierExtension.setUrl(extensionSetting.getKey());
                    modifierExtension.setValue(codeableConcept);
                }));
    }

    @Override
    protected void initializeR4(T theResource) {
        super.initializeR4(theResource);

        getExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.r4.model.CodeableConcept codeableConcept = new org.hl7.fhir.r4.model.CodeableConcept()
                            .addCoding(new org.hl7.fhir.r4.model.Coding()
                                    .setSystem(coding.getSystem())
                                    .setCode(coding.getCode())
                                    .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> extension = theResource.addExtension();
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
                    IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
                    modifierExtension.setUrl(extensionSetting.getKey());
                    modifierExtension.setValue(codeableConcept);
                }));
    }

    @Override
    protected void initializeR5(T theResource) {
        super.initializeR5(theResource);

        getExtensions().forEach(extensionSetting -> extensionSetting
                .getValue()
                .getCodingSettings()
                .forEach(coding -> {
                    org.hl7.fhir.r5.model.CodeableConcept codeableConcept = new org.hl7.fhir.r5.model.CodeableConcept()
                            .addCoding(new org.hl7.fhir.r5.model.Coding()
                                    .setSystem(coding.getSystem())
                                    .setCode(coding.getCode())
                                    .setDisplay(coding.getDisplay()));
                    IBaseExtension<?, ?> extension = theResource.addExtension();
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
                    IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
                    modifierExtension.setUrl(extensionSetting.getKey());
                    modifierExtension.setValue(codeableConcept);
                }));
    }
}
