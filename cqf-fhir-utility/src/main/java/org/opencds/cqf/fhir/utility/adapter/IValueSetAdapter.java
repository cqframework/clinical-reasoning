package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

/**
 * This interface exposes common functionality across all FHIR ValueSet versions.
 */
public interface IValueSetAdapter extends IKnowledgeArtifactAdapter {
    IValueSetAdapter addUseContext(IUsageContextAdapter usageContext);

    <T extends IBaseBackboneElement> void setExpansion(T expansion);

    <T extends IBaseBackboneElement> T getExpansion();

    boolean hasExpansion();

    boolean hasExpansionContains();

    int getExpansionTotal();

    List<IValueSetExpansionContainsAdapter> getExpansionContains();

    void appendExpansionContains(List<IValueSetExpansionContainsAdapter> expansionContains);

    <T extends IBaseBackboneElement> T newExpansion();

    void addExpansionStringParameter(String name, String value);

    boolean hasExpansionStringParameter(String name, String value);

    List<IValueSetConceptSetAdapter> getComposeInclude();

    List<String> getValueSetIncludes();

    boolean hasCompose();

    boolean hasComposeInclude();

    /**
     * A simple compose element of a ValueSet must have a compose without an exclude element. Each element of the
     * include cannot have a filter or reference a ValueSet and must have a system and enumerate concepts.
     *
     * @return boolean
     */
    boolean hasSimpleCompose();

    /**
     * A grouping compose element of a ValueSet must have a compose without an exclude element and each element of the
     * include must reference a ValueSet.
     *
     * @return boolean
     */
    boolean hasGroupingCompose();

    /**
     * Indicates whether this ValueSet's compose element contains explicitly enumerated concepts.
     *
     * <p>This method returns {@code true} if any {@code compose.include} component defines one or more
     * {@code concept} entries. Explicit concepts represent directly specified codes that may be
     * expanded locally using naive expansion, without requiring a terminology service.
     *
     * <p>This method is used by expansion logic to determine whether local expansion should include
     * explicitly defined codes. A ValueSet may contain both explicit concepts and referenced
     * ValueSets (a hybrid compose), in which case both sources must be included in the expansion.
     *
     * <p>This method does not evaluate exclude elements or filters. Presence of filters may still
     * require terminology service expansion depending on implementation capabilities.
     *
     * @return {@code true} if the ValueSet compose includes explicitly enumerated concepts;
     *         {@code false} otherwise
     */
    boolean hasExplicitConcepts();

    /**
     * Indicates whether this ValueSet's compose element includes references to other ValueSets.
     *
     * <p>This method returns {@code true} if any {@code compose.include} component specifies one or
     * more {@code valueSet} canonical references. Referenced ValueSets must be expanded and their
     * resulting codes incorporated into this ValueSet's expansion.
     *
     * <p>This method is used by expansion logic to determine whether recursive expansion of dependent
     * ValueSets is required. A ValueSet may contain both referenced ValueSets and explicitly enumerated
     * concepts (a hybrid compose), in which case both must be expanded and merged.
     *
     * <p>This method does not evaluate exclude elements or filters. Referenced ValueSets that require
     * terminology service expansion may still be delegated depending on implementation capabilities.
     *
     * @return {@code true} if the ValueSet compose includes references to other ValueSets;
     *         {@code false} otherwise
     */
    boolean hasValueSetReferences();

    /**
     * Performs a naive expansion on the ValueSet by collecting all codes within the compose.  Can only be performed on a ValueSet with a simple compose.
     */
    void naiveExpand();

    boolean hasNaiveParameter();

    <T extends IBaseBackboneElement> T createNaiveParameter();
}
