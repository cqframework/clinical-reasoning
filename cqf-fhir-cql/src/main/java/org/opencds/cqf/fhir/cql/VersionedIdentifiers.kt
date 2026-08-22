package org.opencds.cqf.fhir.cql

import org.hl7.elm.r1.VersionedIdentifier

object VersionedIdentifiers {
    @JvmStatic
    fun forUrl(url: String): VersionedIdentifier {
        require(!(!url.contains("/Library/") && !url.startsWith("Library/"))) {
            "Invalid resource type for determining library version identifier: Library"
        }

        val urlSplit = url.split("Library/".toRegex()).dropLastWhile { it.isEmpty() }
        require(urlSplit.size <= 2) {
            "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>"
        }

        val cqlName = if (urlSplit.size == 1) urlSplit[0] else urlSplit[1]
        val versionedIdentifier = VersionedIdentifier()
        if (cqlName.contains("|")) {
            val nameVersion = cqlName.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
            val name = nameVersion[0]
            val version = nameVersion[1]
            versionedIdentifier.id = name
            versionedIdentifier.version = version
        } else {
            versionedIdentifier.id = cqlName
        }

        return versionedIdentifier
    }
}
