package org.opencds.cqf.cql.evaluator.translation.manager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
	
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class CacheAwareModelManagerTests {

    @Test
    public void Multiple_resolutions_should_use_cache(){
        var cache = new HashMap<VersionedIdentifier, Model>();
        var cacheSpy = spy(cache);

        var manager = new CacheAwareModelManager(cacheSpy);
        var versionedIdentifier = new VersionedIdentifier().withId("FHIR").withVersion("4.0.0");

        // First resolution should load global cache
        var result = manager.resolveModel(versionedIdentifier);
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