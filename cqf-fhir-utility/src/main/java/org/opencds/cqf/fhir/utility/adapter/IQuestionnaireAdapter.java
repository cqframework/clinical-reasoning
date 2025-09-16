package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
public interface IQuestionnaireAdapter extends IKnowledgeArtifactAdapter {

    boolean hasItem();

    List<IQuestionnaireItemComponentAdapter> getItem();

    void setItem(List<IQuestionnaireItemComponentAdapter> items);

    void addItem(IQuestionnaireItemComponentAdapter item);

    void addItems(List<IQuestionnaireItemComponentAdapter> items);
}
