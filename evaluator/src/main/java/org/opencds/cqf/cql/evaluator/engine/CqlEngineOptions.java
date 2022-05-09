package org.opencds.cqf.cql.evaluator.engine;

import java.util.EnumSet;
import java.util.Set;

import org.opencds.cqf.cql.engine.execution.CqlEngine;

// TODO: Eventually, the cql-engine needs to expose these itself.
public class CqlEngineOptions {
    private Set<CqlEngine.Options> options = EnumSet.noneOf(CqlEngine.Options.class);
    private boolean isDebugLoggingEnabled = false;

    public Set<CqlEngine.Options> getOptions() {
        return this.options;
    }

    public void setOptions(Set<CqlEngine.Options> options) {
        this.options = options;
    }

    public boolean isDebugLoggingEnabled() {
        return this.isDebugLoggingEnabled;
    }

    public void setDebugLoggingEnabled(boolean isDebugLoggingEnabled) {
        this.isDebugLoggingEnabled = isDebugLoggingEnabled;
    }


    public static CqlEngineOptions defaultOptions() {
        CqlEngineOptions result = new CqlEngineOptions();
        result.options.add(CqlEngine.Options.EnableExpressionCaching);
        return result;
    }  
    
}
