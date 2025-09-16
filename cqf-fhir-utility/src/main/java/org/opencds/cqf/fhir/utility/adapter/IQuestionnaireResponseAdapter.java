package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
public interface IQuestionnaireResponseAdapter extends IResourceAdapter {

    boolean hasItem();

    List<IQuestionnaireResponseItemComponentAdapter> getItem();

    void setItem(List<IQuestionnaireResponseItemComponentAdapter> items);

    void addItem(IQuestionnaireResponseItemComponentAdapter item);

    void addItems(List<IQuestionnaireResponseItemComponentAdapter> items);
}
