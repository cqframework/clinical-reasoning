package org.opencds.cqf.fhir.utility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {

    @SuppressWarnings("squid:S5843") // Suppress Sonar regex complexity warning
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^([1-9]\\d*)\\.(\\d+)\\.(\\d+)(?:-([\\dA-Za-z-]+(?:\\.[\\dA-Za-z-]+)*))?(?:\\+([\\dA-Za-z-]+(?:\\.[\\dA-Za-z-]+)*))?$");

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}([-/]?\\d{2}){0,2}$");

    @Override
    public int compare(String v1, String v2) {
        boolean isV1SemVer = isSemVer(v1);
        boolean isV2SemVer = isSemVer(v2);
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

    private boolean isSemVer(String version) {
        return SEMVER_PATTERN.matcher(version).matches();
    }

    private boolean isDate(String version) {
        return DATE_PATTERN.matcher(version).matches();
    }

    private int compareSemVer(String v1, String v2) {
        String[] main1 = v1.split("-", 2);
        String[] main2 = v2.split("-", 2);

        String[] core1 = main1[0].split("\\.");
        String[] core2 = main2[0].split("\\.");

        for (int i = 0; i < 3; i++) {
            int n1 = i < core1.length ? Integer.parseInt(core1[i]) : 0;
            int n2 = i < core2.length ? Integer.parseInt(core2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }

        // Handle pre-release label comparison
        boolean hasPre1 = main1.length > 1;
        boolean hasPre2 = main2.length > 1;

        if (!hasPre1 && !hasPre2) return 0; // both are normal versions
        if (!hasPre1) return 1; // v1 is release, v2 is pre-release
        if (!hasPre2) return -1; // v1 is pre-release, v2 is release

        return comparePreRelease(main1[1], main2[1]);
    }

    private int comparePreRelease(String p1, String p2) {
        String[] parts1 = p1.split("\\.");
        String[] parts2 = p2.split("\\.");

        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            String s1 = i < parts1.length ? parts1[i] : "";
            String s2 = i < parts2.length ? parts2[i] : "";

            boolean isNum1 = s1.matches("\\d+");
            boolean isNum2 = s2.matches("\\d+");

            if (isNum1 && isNum2) {
                int n1 = Integer.parseInt(s1);
                int n2 = Integer.parseInt(s2);
                if (n1 != n2) return Integer.compare(n1, n2);
            } else if (isNum1) {
                return -1; // numeric < alphanumeric
            } else if (isNum2) {
                return 1; // alphanumeric > numeric
            } else {
                int cmp = s1.compareTo(s2);
                if (cmp != 0) return cmp;
            }
        }

        return 0;
    }

    private int compareDate(String d1, String d2) {
        LocalDate date1 = parseDate(d1);
        LocalDate date2 = parseDate(d2);
        if (date1 == null || date2 == null) {
            return d1.compareTo(d2); // fallback if parse fails
        }
        return date1.compareTo(date2);
    }

    private LocalDate parseDate(String input) {
        String[] formats = {"yyyyMMdd", "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMM", "yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(input, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException e) {
                // try next
            }
        }
        return null;
    }
}
