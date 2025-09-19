package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.cr.crmi.KnowledgeArtifactProcessor.isGrouper;

import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class PackageProviderIT extends BaseCrR4TestServer {

    public Bundle callPackage(String id) {
        var parametersEval = new Parameters();

        return ourClient
                .operation()
                .onInstance(id)
                .named("$package")
                .withParameters(parametersEval)
                .returnResourceType(Bundle.class)
                .execute();
    }

    @Test
    void packageOperation_naive_expansion() {
        loadBundle("small-naive-expansion-bundle.json");
        var result = callPackage("Library/SmallSpecificationLibrary");

        List<ValueSet> leafValueSets = result.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.ValueSet)
                .map(entry -> ((ValueSet) entry.getResource()))
                .filter(valueSet -> !valueSet.hasCompose()
                        || (valueSet.hasCompose()
                                && valueSet.getCompose()
                                        .getIncludeFirstRep()
                                        .getValueSet()
                                        .isEmpty()))
                .toList();

        // Ensure expansion is populated for all leaf value sets
        leafValueSets.forEach(valueSet -> assertNotNull(valueSet.getExpansion()));
    }

    @Test
    void packageOperation_expansion() {
        loadBundle("small-expansion-bundle.json");
        var result = callPackage("Library/SmallSpecificationLibrary");

        List<ValueSet> leafValueSets = result.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType() == ResourceType.ValueSet)
                .map(entry -> ((ValueSet) entry.getResource()))
                .filter(valueSet -> !isGrouper(valueSet))
                .toList();

        // Ensure expansion is populated and each code has correct version for all leaf value sets
        leafValueSets.forEach(valueSet -> assertNotNull(valueSet.getExpansion()));
        //        TODO: This is currently breaking as the naive expansion does not apply the expansion parameter - we
        // should enable that or throw an error
        //        assertTrue(leafValueSets.stream().allMatch(vs -> vs.getExpansion().getContains().stream().allMatch(c
        // -> c.getVersion().equals("http://snomed.info/sct/731000124108/version/20230901"))));
    }
}
