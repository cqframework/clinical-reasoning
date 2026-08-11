package org.opencds.cqf.fhir.cql

import java.util.StringJoiner
import org.opencds.cqf.cql.engine.debug.DebugMap
import org.opencds.cqf.cql.engine.execution.CqlEngine

// TODO: Eventually, the cql-engine needs to expose these itself.
class CqlEngineOptions {
    var options = mutableSetOf<CqlEngine.Options>()
    var isDebugLoggingEnabled: Boolean = false
    var debugMap: DebugMap? = null
    private var shouldExpandValueSets = false
    var pageSize: Int? = null
    var maxCodesPerQuery: Int? = null
    var queryBatchThreshold: Int? = null
    var isEnableHedisCompatibilityMode: Boolean = false

    var isProfilingEnabled: Boolean
        get() = this.options.contains(CqlEngine.Options.EnableProfiling)
        set(enabled) {
            if (enabled) {
                this.options.add(CqlEngine.Options.EnableProfiling)
            } else {
                this.options.remove(CqlEngine.Options.EnableProfiling)
            }
        }

    var isTracingEnabled: Boolean
        get() = this.options.contains(CqlEngine.Options.EnableTracing)
        set(enabled) {
            if (enabled) {
                this.options.add(CqlEngine.Options.EnableTracing)
            } else {
                this.options.remove(CqlEngine.Options.EnableTracing)
            }
        }

    var isCoverageEnabled: Boolean
        get() = this.options.contains(CqlEngine.Options.EnableCoverageCollection)
        set(enabled) {
            if (enabled) {
                this.options.add(CqlEngine.Options.EnableCoverageCollection)
            } else {
                this.options.remove(CqlEngine.Options.EnableCoverageCollection)
            }
        }

    var isDetailedTracingEnabled: Boolean
        get() = this.options.contains(CqlEngine.Options.EnableDetailedTracing)
        set(enabled) {
            if (enabled) {
                this.options.add(CqlEngine.Options.EnableDetailedTracing)
            } else {
                this.options.remove(CqlEngine.Options.EnableDetailedTracing)
            }
        }

    fun shouldExpandValueSets(): Boolean {
        return this.shouldExpandValueSets
    }

    fun setShouldExpandValueSets(shouldExpandValueSets: Boolean) {
        this.shouldExpandValueSets = shouldExpandValueSets
    }

    override fun toString(): String {
        return StringJoiner(", ", CqlEngineOptions::class.java.simpleName + "[", "]")
            .add("options=$options")
            .add("isDebugLoggingEnabled=$isDebugLoggingEnabled")
            .add("debugMap=" + (if (debugMap != null) "configured" else "null"))
            .add("shouldExpandValueSets=$shouldExpandValueSets")
            .add("pageSize=$pageSize")
            .add("maxCodesPerQuery=$maxCodesPerQuery")
            .add("queryBatchThreshold=$queryBatchThreshold")
            .add("enableHedisCompatibilityMode=" + this.isEnableHedisCompatibilityMode)
            .toString()
    }

    companion object {
        @JvmStatic
        fun defaultOptions(): CqlEngineOptions {
            val result = CqlEngineOptions()
            result.options.add(CqlEngine.Options.EnableExpressionCaching)
            return result
        }
    }
}
