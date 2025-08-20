package org.opencds.cqf.fhir.utility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {

    // Stepwise SemVer regex pieces (Sonar-friendly)
    private static final Pattern CORE_PATTERN = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

    private static final Pattern PRERELEASE_PATTERN = Pattern.compile(
            "^(?:0|[1-9]\\d*|[0-9A-Za-z-][0-9A-Za-z-]*)" + "(?:\\.(?:0|[1-9]\\d*|[0-9A-Za-z-][0-9A-Za-z-]*))*$");

    private static final Pattern BUILD_PATTERN = Pattern.compile("^[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*$");

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}([-/]?\\d{2}){0,2}$");

    @Override
    public int compare(String v1, String v2) {
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

        // Fallback: lexicographic
        return v1.compareTo(v2);
    }

    /**
     * Strict SemVer validation (per semver.org spec).
     */
    public boolean isStrictSemVer(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }

        String[] buildSplit = version.split("\\+", 2);
        String mainAndPre = buildSplit[0];
        String build = buildSplit.length > 1 ? buildSplit[1] : null;

        String[] preSplit = mainAndPre.split("-", 2);
        String core = preSplit[0];
        String pre = preSplit.length > 1 ? preSplit[1] : null;

        if (!CORE_PATTERN.matcher(core).matches()) {
            return false;
        }
        if (pre != null && !PRERELEASE_PATTERN.matcher(pre).matches()) {
            return false;
        }
        if (build != null && !BUILD_PATTERN.matcher(build).matches()) {
            return false;
        }
        return true;
    }

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

        // Compare core parts (major.minor.patch)
        String[] parts1 = core1.split("\\.");
        String[] parts2 = core2.split("\\.");
        for (int i = 0; i < 3; i++) {
            int n1 = Integer.parseInt(parts1[i]);
            int n2 = Integer.parseInt(parts2[i]);
            if (n1 != n2) return Integer.compare(n1, n2);
        }

        // Compare pre-release (if any)
        if (pre1 == null && pre2 == null) return 0;
        if (pre1 == null) return 1; // release > pre-release
        if (pre2 == null) return -1;

        return comparePreRelease(pre1, pre2);
    }

    private int comparePreRelease(String p1, String p2) {
        String[] parts1 = p1.split("\\.");
        String[] parts2 = p2.split("\\.");

        int common = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < common; i++) {
            String s1 = parts1[i];
            String s2 = parts2[i];

            boolean isNum1 = s1.matches("\\d+");
            boolean isNum2 = s2.matches("\\d+");

            if (isNum1 && isNum2) {
                long n1 = Long.parseLong(s1); // numeric identifiers compare numerically
                long n2 = Long.parseLong(s2);
                if (n1 != n2) return Long.compare(n1, n2);
            } else if (isNum1 != isNum2) {
                // numeric identifiers have lower precedence than non-numeric
                return isNum1 ? -1 : 1;
            } else {
                int cmp = s1.compareTo(s2); // ASCII sort for non-numeric identifiers
                if (cmp != 0) return cmp;
            }
        }

        // All compared identifiers equal so far; shorter list has lower precedence
        if (parts1.length != parts2.length) {
            return parts1.length < parts2.length ? -1 : 1;
        }
        return 0;
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
            } catch (DateTimeParseException e) {
                // ignore and try next
            }
        }
        return null;
    }

    // Optional public wrapper to directly test date logic
    public int compareDatesDirect(String d1, String d2) {
        return compareDate(d1, d2);
    }
}
