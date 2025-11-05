package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

// LUKETODO: javadoc
// LUKETODO: do we still need this?
public class StratumPopulationCriteriaResults {

    private final Set<Pair<Set<Object>, Set<Object>>> componentResults;
    private final Set<Object> nonComponentResults;

    static StratumPopulationCriteriaResults forComponent(Collection<StratifierComponentDef> stratifierComponentDefs) {
        return null;
    }

    static StratumPopulationCriteriaResults forComponent(Map<String, CriteriaResult> nonComponentStratifierResults) {
        return null;
    }

    private StratumPopulationCriteriaResults(
            Set<Pair<Set<Object>, Set<Object>>> componentResults, Set<Object> nonComponentResults) {

        if (CollectionUtils.isEmpty(componentResults) && CollectionUtils.isEmpty(nonComponentResults)) {
            throw new InternalErrorException("Both componentResults and nonComponentResults are empty");
        }

        if (!CollectionUtils.isEmpty(componentResults) && !CollectionUtils.isEmpty(nonComponentResults)) {
            throw new InternalErrorException("Both componentResults and nonComponentResults are non-empty");
        }

        this.componentResults = componentResults;
        this.nonComponentResults = nonComponentResults;
    }

    boolean isComponentStratifier() {
        return !this.componentResults.isEmpty();
    }

    Set<Object> getIntersection() {
        return null;
    }
}
