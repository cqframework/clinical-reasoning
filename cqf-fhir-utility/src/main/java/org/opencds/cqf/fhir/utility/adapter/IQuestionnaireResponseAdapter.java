package org.opencds.cqf.fhir.utility.adapter;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

/**
 * This interface exposes common functionality across all FHIR Questionnaire versions.
 */
public interface IQuestionnaireResponseAdapter extends IResourceAdapter {

    IQuestionnaireResponseAdapter setId(String id);

    boolean hasQuestionnaire();

    String getQuestionnaire();

    IPrimitiveType<String> getQuestionnaireCanonical();

    IQuestionnaireResponseAdapter setQuestionnaire(String canonical);

    boolean hasSubject();

    IIdType getSubject();

    IQuestionnaireResponseAdapter setSubject(IIdType subject);

    IQuestionnaireResponseAdapter setAuthored(Date date);

    IQuestionnaireResponseAdapter setStatus(String status);

    boolean hasItem();

    default boolean hasItem(String linkId) {
        return !getItem(linkId).isEmpty();
    }

    List<IQuestionnaireResponseItemComponentAdapter> getItem();

    default List<IQuestionnaireResponseItemComponentAdapter> getItem(String linkId) {
        return getItemsWithLinkId(getItem(), linkId);
    }

    default List<IQuestionnaireResponseItemComponentAdapter> getItemsWithLinkId(
            List<IQuestionnaireResponseItemComponentAdapter> items, String linkId) {
        var matchingItems =
                items.stream().filter(i -> linkId.equals(i.getLinkId())).collect(Collectors.toList());
        items.forEach(i -> {
            if (i.hasItem()) {
                matchingItems.addAll(getItemsWithLinkId(
                        i.getItem().stream()
                                .map(IQuestionnaireResponseItemComponentAdapter.class::cast)
                                .collect(Collectors.toList()),
                        linkId));
            }
        });
        return matchingItems;
    }

    void setItem(List<IQuestionnaireResponseItemComponentAdapter> items);

    void addItem(IQuestionnaireResponseItemComponentAdapter item);

    void addItems(List<IQuestionnaireResponseItemComponentAdapter> items);
}
