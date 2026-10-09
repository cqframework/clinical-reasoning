package org.opencds.cqf.fhir.utility

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.min

class VersionComparator : Comparator<String> {
    override fun compare(v1: String, v2: String): Int {
        val isV1SemVer = isStrictSemVer(v1)
        val isV2SemVer = isStrictSemVer(v2)
        if (isV1SemVer && isV2SemVer) {
            return compareSemVer(v1, v2)
        }

        val isV1Date = isDate(v1)
        val isV2Date = isDate(v2)
        if (isV1Date && isV2Date) {
            return compareDate(v1, v2)
        }

        // Fallback to lexicographic
        return v1.compareTo(v2)
    }

    /** -------- Strict SemVer validation without complex regexes -------- */
    fun isStrictSemVer(version: String?): Boolean {
        if (version.isNullOrEmpty()) return false

        // Split build metadata
        val buildSplit = version.split("+", limit = 2)
        val mainAndPre = buildSplit[0]
        val build = if (buildSplit.size > 1) buildSplit[1] else null

        // Split pre-release
        val preSplit = mainAndPre.split("-", limit = 2)
        val core = preSplit[0]
        val pre = if (preSplit.size > 1) preSplit[1] else null

        return (isValidCore(core)) &&
            (pre == null || isValidPreRelease(pre)) &&
            (build == null || isValidBuild(build))
    }

    private fun isValidCore(core: String): Boolean {
        val p = core.split(".")
        if (p.size != 3) return false
        return isValidNumericCorePart(p[0]) &&
            isValidNumericCorePart(p[1]) &&
            isValidNumericCorePart(p[2])
    }

    // major/minor/patch: digits only, no leading zeros unless exactly "0"
    private fun isValidNumericCorePart(s: String?): Boolean {
        if (s.isNullOrEmpty()) return false
        if (s == "0") return true
        if (s[0] == '0') return false
        return allDigits(s)
    }

    private fun isValidPreRelease(pre: String): Boolean {
        val ids = pre.split(".")
        if (ids.isEmpty()) return false
        for (id in ids) {
            if (!isValidPreId(id)) return false
        }
        return true
    }

    // pre-release id: either numeric (no leading zeros unless "0") OR
    // alphanum with at least one letter or hyphen; only [0-9A-Za-z-]
    private fun isValidPreId(s: String?): Boolean {
        if (s.isNullOrEmpty()) return false
        if (!allAlphaNumHyphen(s)) return false

        if (allDigits(s)) {
            // numeric identifier
            return s == "0" || s[0] != '0'
        }
        // must contain at least one non-digit (letter or hyphen)
        return containsLetterOrHyphen(s)
    }

    private fun isValidBuild(build: String): Boolean {
        val ids = build.split(".")
        if (ids.isEmpty()) return false
        for (id in ids) {
            if (id.isEmpty() || !allAlphaNumHyphen(id)) return false
        }
        return true
    }

    private fun allDigits(s: String): Boolean {
        for (i in s.indices) {
            val c = s[i]
            if (c !in '0'..'9') return false
        }
        return true
    }

    private fun containsLetterOrHyphen(s: String): Boolean {
        for (i in s.indices) {
            val c = s[i]
            if ((c in 'A'..'Z') || (c in 'a'..'z') || c == '-') return true
        }
        return false
    }

    private fun allAlphaNumHyphen(s: String): Boolean {
        for (i in s.indices) {
            val c = s[i]
            val ok = (c in '0'..'9') || (c in 'A'..'Z') || (c in 'a'..'z') || c == '-'
            if (!ok) return false
        }
        return true
    }

    /** --------------------- Comparison logic --------------------- */
    private fun isDate(version: String): Boolean {
        return DATE_PATTERN.matches(version)
    }

    private fun compareSemVer(v1: String, v2: String): Int {
        val buildSplit1 = v1.split("+", limit = 2)
        val buildSplit2 = v2.split("+", limit = 2)

        val mainAndPre1 = buildSplit1[0]
        val mainAndPre2 = buildSplit2[0]

        val preSplit1 = mainAndPre1.split("-", limit = 2)
        val preSplit2 = mainAndPre2.split("-", limit = 2)

        val core1 = preSplit1[0]
        val core2 = preSplit2[0]

        val pre1 = if (preSplit1.size > 1) preSplit1[1] else null
        val pre2 = if (preSplit2.size > 1) preSplit2[1] else null

        // Compare core (major.minor.patch)
        val parts1 = core1.split(".")
        val parts2 = core2.split(".")
        for (i in 0..2) {
            val n1 = parts1[i].toInt()
            val n2 = parts2[i].toInt()
            if (n1 != n2) return n1.compareTo(n2)
        }

        // Pre-release comparison
        if (pre1 == null && pre2 == null) return 0
        if (pre1 == null) return 1 // release > pre-release

        if (pre2 == null) return -1

        return comparePreRelease(pre1, pre2)
    }

    // compare identifiers left-to-right; numeric < non-numeric; if all equal, shorter is lower.
    private fun comparePreRelease(p1: String, p2: String): Int {
        val parts1 = p1.split(".")
        val parts2 = p2.split(".")

        val common = min(parts1.size, parts2.size)
        for (i in 0..<common) {
            val s1 = parts1[i]
            val s2 = parts2[i]

            val isNum1 = allDigits(s1)
            val isNum2 = allDigits(s2)

            if (isNum1 && isNum2) {
                val n1 = s1.toLong()
                val n2 = s2.toLong()
                if (n1 != n2) return n1.compareTo(n2)
            } else if (isNum1 != isNum2) {
                return if (isNum1) -1 else 1 // numeric < non-numeric
            } else {
                val cmp = s1.compareTo(s2) // ASCII
                if (cmp != 0) return cmp
            }
        }
        // All equal so far; shorter list has lower precedence
        return parts1.size.compareTo(parts2.size)
    }

    private fun compareDate(d1: String, d2: String): Int {
        val date1 = parseDate(d1)
        val date2 = parseDate(d2)
        if (date1 == null || date2 == null) {
            return d1.compareTo(d2)
        }
        return date1.compareTo(date2)
    }

    private fun parseDate(input: String): LocalDate? {
        val formats = listOf("yyyyMMdd", "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMM", "yyyy")
        for (fmt in formats) {
            try {
                return LocalDate.parse(input, DateTimeFormatter.ofPattern(fmt))
            } catch (ignored: DateTimeParseException) {
                // Expected: try next format
            }
        }
        return null
    }

    // Public wrapper for tests
    fun compareDatesDirect(d1: String, d2: String): Int {
        return compareDate(d1, d2)
    }

    companion object {
        private val DATE_PATTERN = Regex("^\\d{4}([-/]?\\d{2}){0,2}$")
    }
}
