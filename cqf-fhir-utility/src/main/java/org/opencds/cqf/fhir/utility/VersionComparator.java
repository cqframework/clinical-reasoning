package org.opencds.cqf.fhir.utility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}([-/]?\\d{2}){0,2}$");

    @Override
    public int compare(String v1, String v2) {
        // Handle null values: nulls are considered "less than" non-nulls
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        boolean isV1SemVer = isStrictSemVer(v1);
        boolean isV2SemVer = isStrictSemVer(v2);
        if (isV1SemVer && isV2SemVer) {
            return compareSemVer(v1, v2);
        }

        boolean isV1Date = isDate(v1);
        boolean isV2Date = isDate(v2);
        if (isV1Date && isV2Date) {
            return compareDate(v1, v2);
        }

        // Fallback to lexicographic
        return v1.compareTo(v2);
    }

    /** -------- Strict SemVer validation without complex regexes -------- */
    public boolean isStrictSemVer(String version) {
        if (version == null || version.isEmpty()) return false;

        // Split build metadata
        String[] buildSplit = version.split("\\+", 2);
        String mainAndPre = buildSplit[0];
        String build = (buildSplit.length > 1) ? buildSplit[1] : null;

        // Split pre-release
        String[] preSplit = mainAndPre.split("-", 2);
        String core = preSplit[0];
        String pre = (preSplit.length > 1) ? preSplit[1] : null;

        return (isValidCore(core)) && (pre == null || isValidPreRelease(pre)) && (build == null || isValidBuild(build));
    }

    private boolean isValidCore(String core) {
        String[] p = core.split("\\.", -1);
        if (p.length != 3) return false;
        return isValidNumericCorePart(p[0]) && isValidNumericCorePart(p[1]) && isValidNumericCorePart(p[2]);
    }

    // major/minor/patch: digits only, no leading zeros unless exactly "0"
    private boolean isValidNumericCorePart(String s) {
        if (s == null || s.isEmpty()) return false;
        if (s.equals("0")) return true;
        if (s.charAt(0) == '0') return false;
        return allDigits(s);
    }

    private boolean isValidPreRelease(String pre) {
        String[] ids = pre.split("\\.", -1);
        if (ids.length == 0) return false;
        for (String id : ids) {
            if (!isValidPreId(id)) return false;
        }
        return true;
    }

    // pre-release id: either numeric (no leading zeros unless "0") OR
    // alphanum with at least one letter or hyphen; only [0-9A-Za-z-]
    private boolean isValidPreId(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!allAlphaNumHyphen(s)) return false;

        if (allDigits(s)) {
            // numeric identifier
            return s.equals("0") || s.charAt(0) != '0';
        }
        // must contain at least one non-digit (letter or hyphen)
        return containsLetterOrHyphen(s);
    }

    private boolean isValidBuild(String build) {
        String[] ids = build.split("\\.", -1);
        if (ids.length == 0) return false;
        for (String id : ids) {
            if (id.isEmpty() || !allAlphaNumHyphen(id)) return false;
        }
        return true;
    }

    private boolean allDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private boolean containsLetterOrHyphen(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '-') return true;
        }
        return false;
    }

    private boolean allAlphaNumHyphen(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '-';
            if (!ok) return false;
        }
        return true;
    }

    /** --------------------- Comparison logic --------------------- */
    private boolean isDate(String version) {
        return DATE_PATTERN.matcher(version).matches();
    }

    private int compareSemVer(String v1, String v2) {
        String[] buildSplit1 = v1.split("\\+", 2);
        String[] buildSplit2 = v2.split("\\+", 2);

        String mainAndPre1 = buildSplit1[0];
        String mainAndPre2 = buildSplit2[0];

        String[] preSplit1 = mainAndPre1.split("-", 2);
        String[] preSplit2 = mainAndPre2.split("-", 2);

        String core1 = preSplit1[0];
        String core2 = preSplit2[0];

        String pre1 = preSplit1.length > 1 ? preSplit1[1] : null;
        String pre2 = preSplit2.length > 1 ? preSplit2[1] : null;

        // Compare core (major.minor.patch)
        String[] parts1 = core1.split("\\.");
        String[] parts2 = core2.split("\\.");
        for (int i = 0; i < 3; i++) {
            int n1 = Integer.parseInt(parts1[i]);
            int n2 = Integer.parseInt(parts2[i]);
            if (n1 != n2) return Integer.compare(n1, n2);
        }

        // Pre-release comparison
        if (pre1 == null && pre2 == null) return 0;
        if (pre1 == null) return 1; // release > pre-release
        if (pre2 == null) return -1;

        return comparePreRelease(pre1, pre2);
    }

    // compare identifiers left-to-right; numeric < non-numeric; if all equal, shorter is lower.
    private int comparePreRelease(String p1, String p2) {
        String[] parts1 = p1.split("\\.");
        String[] parts2 = p2.split("\\.");

        int common = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < common; i++) {
            String s1 = parts1[i];
            String s2 = parts2[i];

            boolean isNum1 = allDigits(s1);
            boolean isNum2 = allDigits(s2);

            if (isNum1 && isNum2) {
                long n1 = Long.parseLong(s1);
                long n2 = Long.parseLong(s2);
                if (n1 != n2) return Long.compare(n1, n2);
            } else if (isNum1 != isNum2) {
                return isNum1 ? -1 : 1; // numeric < non-numeric
            } else {
                int cmp = s1.compareTo(s2); // ASCII
                if (cmp != 0) return cmp;
            }
        }
        // All equal so far; shorter list has lower precedence
        return Integer.compare(parts1.length, parts2.length);
    }

    private int compareDate(String d1, String d2) {
        LocalDate date1 = parseDate(d1);
        LocalDate date2 = parseDate(d2);
        if (date1 == null || date2 == null) {
            return d1.compareTo(d2);
        }
        return date1.compareTo(date2);
    }

    private LocalDate parseDate(String input) {
        String[] formats = {"yyyyMMdd", "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMM", "yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(input, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {
                // Expected: try next format
            }
        }
        return null;
    }

    // Public wrapper for tests
    public int compareDatesDirect(String d1, String d2) {
        return compareDate(d1, d2);
    }
}
