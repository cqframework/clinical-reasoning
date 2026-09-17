package org.opencds.cqf.fhir.utility

import java.util.function.Function
import kotlin.NumberFormatException
import kotlin.math.max
import org.hl7.fhir.instance.model.api.IBaseResource

/** This class provides utilities for handling multiple business versions of FHIR Resources. */
object Versions {
    /**
     * This function compares two versions using semantic versioning.
     *
     * @param version1 the first version to compare
     * @param version2 the second version to compare
     * @return 0 if versions are equal, 1 if version1 is greater than version2, and -1 otherwise
     */
    @JvmStatic
    fun compareVersions(version1: String?, version2: String?): Int {
        // Treat null as MAX VERSION
        if (version1 == null || version2 == null) {
            return handleNulls(version1, version2)
        }

        val v1Valid = isValidSemver(version1)
        val v2Valid = isValidSemver(version2)

        if (!v1Valid || !v2Valid) {
            return handleInvalids(version1, version2, v1Valid, v2Valid)
        }

        val string1Vals = version1.split(".").dropLastWhile { it.isEmpty() }
        val string2Vals = version2.split(".").dropLastWhile { it.isEmpty() }

        val length = max(string1Vals.size, string2Vals.size)

        for (i in 0..<length) {
            if (i > string1Vals.size - 1) {
                return -1
            }
            if (i > string2Vals.size - 1) {
                return 1
            }
            if (i == length - 1) {
                break
            }
            if (string1Vals[i] != string2Vals[i]) {
                return Versions.stringOrNumberCompare(string1Vals[i], string2Vals[i])
            }
        }
        val tail1 = Versions.parseTail(string1Vals[length - 1])
        val tail2 = Versions.parseTail(string2Vals[length - 1])

        return if (tail1.first == tail2.first) {
            compareTails(tail1, tail2)
        } else {
            Versions.intCompare(tail1.first, tail2.first)
        }
    }

    private fun compareTails(tail1: Pair<Int, String>, tail2: Pair<Int, String>): Int {
        return if (tail1.second.isNotEmpty() && tail2.second.isEmpty()) {
            1
        } else if (tail1.second.isEmpty() && tail2.second.isNotEmpty()) {
            -1
        } else {
            val c: Int = tail1.second.compareTo(tail2.second)
            // compareTo returns numbers outside [-1,1]
            if (c > 0) {
                1
            } else if (c < 0) {
                -1
            } else {
                0
            }
        }
    }

    private fun handleNulls(version1: String?, version2: String?): Int {
        return if (version1 == null && version2 == null) {
            0
        } else if (version1 != null && version2 == null) {
            -1
        } else {
            1
        }
    }

    private fun handleInvalids(
        version1: String,
        version2: String,
        v1Valid: Boolean,
        v2Valid: Boolean,
    ): Int {
        return if (!v1Valid && !v2Valid) {
            stringOrNumberCompare(version1, version2)
        } else if (v1Valid && !v2Valid) {
            -1
        } else {
            1
        }
    }

    private fun stringOrNumberCompare(version1: String, version2: String): Int {
        // try string and number compares if it's not semver
        try {
            val d1 = version1.toInt()
            val d2 = version2.toInt()
            return intCompare(d1, d2)
        } catch (e: NumberFormatException) {
            val c = version1.compareTo(version2)
            // compareTo returns numbers outside [-1,1]
            return if (c > 0) {
                1
            } else if (c < 0) {
                -1
            } else {
                0
            }
        }
    }

    private fun intCompare(d1: Int, d2: Int): Int {
        return if (d1 > d2) {
            1
        } else if (d2 > d1) {
            -1
        } else {
            0
        }
    }

    private fun isValidSemver(check: String): Boolean {
        if (check.length > 1 && !check.contains(".")) {
            return false
        }
        val stringVals = check.split(".").dropLastWhile { it.isEmpty() }
        for (i in 0..<stringVals.size - 1) {
            try {
                stringVals[i].toInt()
            } catch (e: NumberFormatException) {
                return false
            }
        }
        try {
            Versions.parseTail(stringVals[stringVals.size - 1])
        } catch (e: NumberFormatException) {
            return false
        }
        return true
    }

    private fun parseTail(tail: String): Pair<Int, String> {
        try {
            return tail.toInt() to ""
        } catch (e: NumberFormatException) {
            if (tail.contains("-")) {
                val splitDash = tail.split("-").dropLastWhile { it.isEmpty() }
                val afterDash = splitDash.slice(1..<splitDash.size).joinToString("-")
                return splitDash[0].toInt() to afterDash
            } else {
                throw e
            }
        }
    }

    /**
     * Given a list of FHIR Resources that have the same name, choose the one with the matching
     * version.
     *
     * @param <ResourceType> an IBaseResource type
     * @param resources a list of Resources to select from
     * @param version the version of the Resource to select
     * @param getVersion a function to access version information for the ResourceType
     * @return the Resource with a matching version, or the highest version orwise. </ResourceType>
     */
    @JvmStatic
    fun <ResourceType : IBaseResource?> selectByVersion(
        resources: List<ResourceType?>,
        version: String?,
        getVersion: Function<ResourceType?, String?>,
    ): ResourceType? {

        var library: ResourceType? = null
        var maxVersion: ResourceType? = null
        for (l in resources) {
            val currentVersion = getVersion.apply(l)
            if (
                version == null && currentVersion == null ||
                    version != null && version == currentVersion
            ) {
                library = l
            }

            if (
                maxVersion == null ||
                    compareVersions(currentVersion, getVersion.apply(maxVersion)) >= 0
            ) {
                maxVersion = l
            }
        }

        // If we were not given a version, return the highest found
        if ((version == null || library == null) && maxVersion != null) {
            return maxVersion
        }

        return library
    }
}
