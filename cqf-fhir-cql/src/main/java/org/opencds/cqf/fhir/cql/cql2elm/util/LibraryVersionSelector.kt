package org.opencds.cqf.fhir.cql.cql2elm.util

import kotlin.math.max
import org.hl7.elm.r1.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter

class LibraryVersionSelector(private val adapterFactory: IAdapterFactory) {
    fun select(
        libraryIdentifier: VersionedIdentifier,
        libraries: Collection<IBaseResource?>,
    ): IBaseResource? {
        val targetVersion = libraryIdentifier.version

        val adapters = libraries.map { x -> this.adapterFactory.createLibrary(x) }

        var library: ILibraryAdapter? = null
        var maxLibrary: ILibraryAdapter? = null

        for (l in adapters) {
            if (l.name != libraryIdentifier.id) {
                continue
            }

            val currentVersion = l.version
            if (
                (targetVersion != null &&
                    currentVersion != null &&
                    currentVersion == targetVersion) ||
                    (targetVersion == null && currentVersion == null)
            ) {
                library = l
            }

            if (maxLibrary == null || compareVersions(maxLibrary.version, currentVersion) < 0) {
                maxLibrary = l
            }
        }

        if (targetVersion == null && maxLibrary != null) {
            library = maxLibrary
        }

        if (library == null) {
            return null
        }

        return library.get()
    }

    companion object {
        fun compareVersions(version1: String?, version2: String?): Int {
            // Treat null as MAX VERSION
            if (version1 == null && version2 == null) {
                return 0
            }

            if (version1 != null && version2 == null) {
                return -1
            }

            if (version1 == null) {
                return 1
            }

            val string1Values = version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            val string2Values = version2!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }

            val length = max(string1Values.size, string2Values.size)

            for (i in 0..<length) {
                val v1 = if (i < string1Values.size) string1Values[i].toInt() else 0
                val v2 = if (i < string2Values.size) string2Values[i].toInt() else 0

                // Making sure Version1 bigger than version2
                if (v1 > v2) {
                    return 1
                } else if (v1 < v2) {
                    return -1
                }
            }

            // Both are equal
            return 0
        }
    }
}
