package org.opencds.cqf.fhir.cql

import org.cqframework.cql.cql2elm.CqlCompilerOptions
import org.cqframework.cql.cql2elm.LibraryBuilder.SignatureLevel

// TODO: Migrate upstream to engine project. Or is it duplicated already?
class CqlOptions {
    var cqlCompilerOptions: CqlCompilerOptions = CqlCompilerOptions.defaultOptions()
    var cqlEngineOptions: CqlEngineOptions = CqlEngineOptions.defaultOptions()
        private set

    private var useEmbeddedLibraries = true

    fun setCqlEngineOptions(cqlEngineOptions: CqlEngineOptions): CqlOptions {
        this.cqlEngineOptions = cqlEngineOptions
        return this
    }

    fun useEmbeddedLibraries(): Boolean {
        return this.useEmbeddedLibraries
    }

    fun setUseEmbeddedLibraries(useEmbeddedLibraries: Boolean): CqlOptions {
        this.useEmbeddedLibraries = useEmbeddedLibraries
        return this
    }

    companion object {
        @JvmStatic
        fun defaultOptions(): CqlOptions {
            val opt = CqlOptions()
            opt.cqlCompilerOptions.signatureLevel = SignatureLevel.All
            opt.cqlCompilerOptions.options.add(CqlCompilerOptions.Options.EnableLocators)
            opt.cqlCompilerOptions.options.add(CqlCompilerOptions.Options.EnableAnnotations)
            return opt
        }
    }
}
