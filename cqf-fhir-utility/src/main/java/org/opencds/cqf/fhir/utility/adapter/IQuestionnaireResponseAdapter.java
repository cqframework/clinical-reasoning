package org.opencds.cqf.fhir.utility.adapter;

import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
public interface IQuestionnaireResponseAdapter extends IResourceAdapter {

    IQuestionnaireResponseAdapter setId(String id);

    IQuestionnaireResponseAdapter setQuestionnaire(String canonical);

    IQuestionnaireResponseAdapter setSubject(IIdType subject);

    IQuestionnaireResponseAdapter setAuthored(Date date);

    IQuestionnaireResponseAdapter setStatus(String status);

    boolean hasItem();

    List<IQuestionnaireResponseItemComponentAdapter> getItem();

    void setItem(List<IQuestionnaireResponseItemComponentAdapter> items);

    void addItem(IQuestionnaireResponseItemComponentAdapter item);

    void addItems(List<IQuestionnaireResponseItemComponentAdapter> items);
}
