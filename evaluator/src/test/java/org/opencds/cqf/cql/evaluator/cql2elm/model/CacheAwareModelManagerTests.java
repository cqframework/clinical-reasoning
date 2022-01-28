package org.opencds.cqf.cql.evaluator.cql2elm.model;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.testng.annotations.Test;

public class CacheAwareModelManagerTests {

    @Test
    public void Multiple_resolutions_should_use_cache(){
        Map<VersionedIdentifier, Model> cache = new HashMap<VersionedIdentifier, Model>();
        Map<VersionedIdentifier, Model> cacheSpy = spy(cache);

        ModelManager manager = new CacheAwareModelManager(cacheSpy);
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier().withId("FHIR").withVersion("4.0.0");

        // First resolution should load global cache
        Model result = manager.resolveModel(versionedIdentifier);
        assertNotNull(result);

        verify(cacheSpy, times(1)).containsKey(versionedIdentifier);
        verify(cacheSpy, times(1)).put(versionedIdentifier, result);
        

        // Second resolution should not use global cache
        result = manager.resolveModel(versionedIdentifier);
        assertNotNull(result);

        verify(cacheSpy, times(1)).containsKey(versionedIdentifier);
        verify(cacheSpy, times(1)).put(versionedIdentifier, result);
    }
       
}