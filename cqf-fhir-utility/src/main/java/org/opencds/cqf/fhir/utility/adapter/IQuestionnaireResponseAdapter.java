package org.opencds.cqf.fhir.utility.adapter;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseReference;
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

    default List<IBaseReference> getBasedOn() {
        return resolvePathList(get(), "basedOn", IBaseReference.class);
    }

    default List<IBaseReference> getPartOf() {
        return resolvePathList(get(), "partOf", IBaseReference.class);
    }

    boolean hasSubject();

    IIdType getSubject();

    IQuestionnaireResponseAdapter setSubject(IIdType subject);

    default boolean hasEncounter() {
        return getEncounter() != null;
    }

    default IBaseReference getEncounter() {
        return resolvePath("encounter", IBaseReference.class);
    }

    default IQuestionnaireResponseAdapter setEncounter(IBaseReference encounter) {
        setValue("encounter", encounter);
        return this;
    }

    default boolean hasAuthored() {
        return getAuthored() != null;
    }

    default Date getAuthored() {
        var authored = resolvePath("authored", IPrimitiveType.class);
        return authored == null ? null : (Date) authored.getValue();
    }

    IQuestionnaireResponseAdapter setAuthored(Date date);

    default boolean hasAuthor() {
        return getAuthor() != null;
    }

    default IBaseReference getAuthor() {
        return resolvePath("author", IBaseReference.class);
    }

    IQuestionnaireResponseAdapter setStatus(String status);

    default String getStatus() {
        return resolvePathString("status");
    }

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
                                .toList(),
                        linkId));
            }
        });
        return matchingItems;
    }

    void setItem(List<IQuestionnaireResponseItemComponentAdapter> items);

    void addItem(IQuestionnaireResponseItemComponentAdapter item);

    void addItems(List<IQuestionnaireResponseItemComponentAdapter> items);
}
