package org.opencds.cqf.fhir.cql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.CqlEngine;

class CqlEngineOptionsTest {

    @Test
    void defaultOptionsHasExpressionCaching() {
        var options = CqlEngineOptions.defaultOptions();
        assertTrue(options.getOptions().contains(CqlEngine.Options.EnableExpressionCaching));
    }

    @Test
    void debugMapDefaultsToNull() {
        var options = CqlEngineOptions.defaultOptions();
        assertNull(options.getDebugMap());
    }

    @Test
    void debugMapGetterSetter() {
        var options = CqlEngineOptions.defaultOptions();
        var debugMap = new DebugMap();
        options.setDebugMap(debugMap);
        assertSame(debugMap, options.getDebugMap());
    }

    @Test
    void profilingEnabled() {
        var options = CqlEngineOptions.defaultOptions();
        assertFalse(options.isProfilingEnabled());
        assertFalse(options.getOptions().contains(CqlEngine.Options.EnableProfiling));

        options.setProfilingEnabled(true);
        assertTrue(options.isProfilingEnabled());
        assertTrue(options.getOptions().contains(CqlEngine.Options.EnableProfiling));

        options.setProfilingEnabled(false);
        assertFalse(options.isProfilingEnabled());
        assertFalse(options.getOptions().contains(CqlEngine.Options.EnableProfiling));
    }

    @Test
    void tracingEnabled() {
        var options = CqlEngineOptions.defaultOptions();
        assertFalse(options.isTracingEnabled());
        assertFalse(options.getOptions().contains(CqlEngine.Options.EnableTracing));

        options.setTracingEnabled(true);
        assertTrue(options.isTracingEnabled());
        assertTrue(options.getOptions().contains(CqlEngine.Options.EnableTracing));

        options.setTracingEnabled(false);
        assertFalse(options.isTracingEnabled());
        assertFalse(options.getOptions().contains(CqlEngine.Options.EnableTracing));
    }

    @Test
    void coverageEnabled() {
        var options = CqlEngineOptions.defaultOptions();
        assertFalse(options.isCoverageEnabled());
        assertFalse(options.getOptions().contains(CqlEngine.Options.EnableCoverageCollection));

        options.setCoverageEnabled(true);
        assertTrue(options.isCoverageEnabled());
        assertTrue(options.getOptions().contains(CqlEngine.Options.EnableCoverageCollection));

        options.setCoverageEnabled(false);
        assertFalse(options.isCoverageEnabled());
        assertFalse(options.getOptions().contains(CqlEngine.Options.EnableCoverageCollection));
    }

    @Test
    void convenienceMethodsPreserveExpressionCaching() {
        var options = CqlEngineOptions.defaultOptions();
        assertTrue(options.getOptions().contains(CqlEngine.Options.EnableExpressionCaching));

        options.setProfilingEnabled(true);
        options.setTracingEnabled(true);
        options.setCoverageEnabled(true);

        assertTrue(options.getOptions().contains(CqlEngine.Options.EnableExpressionCaching));
        assertTrue(options.isProfilingEnabled());
        assertTrue(options.isTracingEnabled());
        assertTrue(options.isCoverageEnabled());

        options.setProfilingEnabled(false);
        options.setTracingEnabled(false);
        options.setCoverageEnabled(false);

        assertTrue(options.getOptions().contains(CqlEngine.Options.EnableExpressionCaching));
    }

    @Test
    void toStringWithoutDebugMap() {
        var options = CqlEngineOptions.defaultOptions();
        String result = options.toString();
        assertNotNull(result);
        assertTrue(result.contains("debugMap=null"));
    }

    @Test
    void toStringWithDebugMap() {
        var options = CqlEngineOptions.defaultOptions();
        options.setDebugMap(new DebugMap());
        String result = options.toString();
        assertNotNull(result);
        assertTrue(result.contains("debugMap=configured"));
    }
}
