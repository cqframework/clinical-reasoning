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
     * Performs a naive expansion on the ValueSet by collecting all codes within the compose.  Can only be performed on a ValueSet with a simple compose.
     */
    void naiveExpand();

    boolean hasNaiveParameter();

    <T extends IBaseBackboneElement> T createNaiveParameter();
}
