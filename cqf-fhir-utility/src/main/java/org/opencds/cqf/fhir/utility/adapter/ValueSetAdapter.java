package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

/**
 * This interface exposes common functionality across all FHIR ValueSet versions.
 */
public interface ValueSetAdapter extends KnowledgeArtifactAdapter {
    public <T extends IBaseBackboneElement> void setExpansion(T expansion);

    public <T extends IBaseBackboneElement> T getExpansion();

    public <T extends IBaseBackboneElement> T newExpansion();

    public List<String> getValueSetIncludes();

    /**
     * A simple compose element of a ValueSet must have a compose without an exclude element. Each element of the
     * include cannot have a filter or reference a ValueSet and must have a system and enumerate concepts.
     *
     * @return boolean
     */
    public boolean hasSimpleCompose();

    /**
     * A grouping compose element of a ValueSet must have a compose without an exclude element and each element of the
     * include must reference a ValueSet.
     *
     * @return boolean
     */
    public boolean hasGroupingCompose();

    /**
     * Performs a naive expansion on the ValueSet by collecting all codes within the compose.  Can only be performed on a ValueSet with a simple compose.
     */
    public void naiveExpand();

    public boolean hasNaiveParameter();

    public <T extends IBaseBackboneElement> T createNaiveParameter();
}
