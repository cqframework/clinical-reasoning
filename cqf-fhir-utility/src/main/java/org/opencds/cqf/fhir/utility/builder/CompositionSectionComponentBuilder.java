package org.opencds.cqf.fhir.utility.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public class CompositionSectionComponentBuilder<T extends IBaseBackboneElement>
        extends BaseBackboneElementBuilder<CompositionSectionComponentBuilder<T>, T> {

    private String title;
    private String focus;
    private List<String> entries;
    private NarrativeSettings text;

    public CompositionSectionComponentBuilder(Class<T> resourceClass) {
        super(resourceClass);
    }

    public CompositionSectionComponentBuilder(Class<T> resourceClass, String id) {
        super(resourceClass, id);
    }

    public CompositionSectionComponentBuilder(Class<T> resourceClass, String id, String focus, String entry) {
        this(resourceClass, id);
        checkNotNull(focus);
        checkNotNull(entry);

        this.focus = focus;
        addEntry(entry);
    }

    private void addEntry(String entry) {
        if (this.entries == null) {
            this.entries = new ArrayList<>();
        }

        this.entries.add(entry);
    }

    private List<String> getEntries() {
        if (entries == null) {
            return Collections.emptyList();
        }

        return entries;
    }

    public CompositionSectionComponentBuilder<T> withTitle(String title) {
        checkNotNull(title);

        this.title = title;

        return this;
    }

    public CompositionSectionComponentBuilder<T> withFocus(String focus) {
        checkNotNull(focus);

        this.focus = focus;

        return this;
    }

    public CompositionSectionComponentBuilder<T> withEntry(String entry) {
        checkNotNull(entry);

        addEntry(entry);

        return this;
    }

    public CompositionSectionComponentBuilder<T> withText(NarrativeSettings text) {
        checkNotNull(text);

        this.text = text;

        return this;
    }

    @Override
    public T build() {
        checkNotNull(focus);
        checkNotNull(entries);
        checkArgument(!entries.isEmpty());

        return super.build();
    }

    @Override
    protected void initializeDstu3(T resource) {
        super.initializeDstu3(resource);

        org.hl7.fhir.dstu3.model.Composition.SectionComponent section =
                (org.hl7.fhir.dstu3.model.Composition.SectionComponent) resource;

        section.setTitle(title).setId(getId());
        getEntries().forEach(entry -> section.addEntry(new org.hl7.fhir.dstu3.model.Reference(entry)));
        if (text != null) {
            org.hl7.fhir.dstu3.model.Narrative narrative = new org.hl7.fhir.dstu3.model.Narrative();
            narrative.setStatusAsString(text.getStatus());
            narrative.setDivAsString(text.getText());
            section.setText(narrative);
        }
        // no focus
    }

    @Override
    protected void initializeR4(T resource) {
        super.initializeR4(resource);
        org.hl7.fhir.r4.model.Composition.SectionComponent section =
                (org.hl7.fhir.r4.model.Composition.SectionComponent) resource;

        section.setFocus(new org.hl7.fhir.r4.model.Reference(focus))
                .setTitle(title)
                .setId(getId());
        getEntries().forEach(entry -> section.addEntry(new org.hl7.fhir.r4.model.Reference(entry)));
        if (text != null) {
            org.hl7.fhir.r4.model.Narrative narrative = new org.hl7.fhir.r4.model.Narrative();
            narrative.setStatusAsString(text.getStatus());
            narrative.setDivAsString(text.getText());
            section.setText(narrative);
        }
    }

    @Override
    protected void initializeR5(T resource) {
        super.initializeR5(resource);
        org.hl7.fhir.r5.model.Composition.SectionComponent section =
                (org.hl7.fhir.r5.model.Composition.SectionComponent) resource;

        section.setFocus(new org.hl7.fhir.r5.model.Reference(focus))
                .setTitle(title)
                .setId(getId());
        getEntries().forEach(entry -> section.addEntry(new org.hl7.fhir.r5.model.Reference(entry)));
        if (text != null) {
            org.hl7.fhir.r5.model.Narrative narrative = new org.hl7.fhir.r5.model.Narrative();
            narrative.setStatusAsString(text.getStatus());
            narrative.setDivAsString(text.getText());
            section.setText(narrative);
        }
    }
}
