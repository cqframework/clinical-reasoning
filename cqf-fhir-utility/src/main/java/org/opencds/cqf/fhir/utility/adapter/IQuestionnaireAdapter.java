package org.opencds.cqf.fhir.utility.adapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
public interface IQuestionnaireAdapter extends IKnowledgeArtifactAdapter {

    boolean hasItem();

    List<IQuestionnaireItemComponentAdapter> getItem();

    void setItem(List<IQuestionnaireItemComponentAdapter> items);

    void addItem(IBaseBackboneElement item);

    void addItem(IQuestionnaireItemComponentAdapter item);

    void addItems(List<IQuestionnaireItemComponentAdapter> items);

    default Set<String> getAllItemDefinitions() {
        return getItemDefs(getItem());
    }

    default Set<String> getItemDefs(List<? extends IItemComponentAdapter> items) {
        var defs = new HashSet<String>();
        items.forEach(item -> {
            if (item.hasDefinition()) {
                defs.add(item.getDefinition());
            }
            if (item.hasItem()) {
                defs.addAll(getItemDefs(item.getItem()));
            }
        });
        return defs;
    }
}
