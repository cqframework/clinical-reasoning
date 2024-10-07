package org.opencds.cqf.fhir.utility;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * This class provides utilities for handling multiple business versions of FHIR Resources.
 */
public class Versions {
    private Versions() {}

    /**
     * This function compares two versions using semantic versioning.
     *
     * @param version1 the first version to compare
     * @param version2 the second version to compare
     * @return 0 if versions are equal, 1 if version1 is greater than version2, and -1 otherwise
     */
    public static int compareVersions(String version1, String version2) {
        // Treat null as MAX VERSION
        if (version1 == null && version2 == null) {
            return 0;
        }

        if (version1 != null && version2 == null) {
            return -1;
        }

        if (version1 == null) {
            return 1;
        }

        final var v1Valid = isValidSemver(version1);
        final var v2Valid = isValidSemver(version2);

        if (!v1Valid && !v2Valid) {
            // try string and number compares if it's not semver
            try {
                final var c = Integer.parseInt(version1) - Integer.parseInt(version2);
                return c > 0 ? 1 : (c < 0 ? -1 : 0);
            } catch (NumberFormatException e) {
                final var c = version1.compareTo(version2);
                // compareTo returns numbers outside [-1,1]
                return c > 0 ? 1 : (c < 0 ? -1 : 0);
            }
        }

        if (v1Valid && !v2Valid) {
            return -1;
        }

        if (!v1Valid) {
            return 1;
        }

        String[] string1Vals = version1.split("\\.");
        String[] string2Vals = version2.split("\\.");

        int length = Math.max(string1Vals.length, string2Vals.length);

        for (int i = 0; i < length; i++) {
            if (i > string1Vals.length - 1) {
                return -1;
            }
            if (i > string2Vals.length - 1) {
                return 1;
            }
            Integer v1 = 0;
            Integer v2 = 0;
            if (i < length - 1) {
                v1 = Integer.parseInt(string1Vals[i]);
                v2 = Integer.parseInt(string2Vals[i]);
            } else {
                // handle "-whatever"
                final var tail1 = parseTail(string1Vals[i]);
                final var tail2 = parseTail(string2Vals[i]);
                if (tail1.getRight().isEmpty() && tail2.getRight().isEmpty()
                        || !tail1.getLeft().equals(tail2.getLeft())) {
                    v1 = tail1.getLeft();
                    v2 = tail2.getLeft();
                    // if theres no "-whatever" then compare like normal
                } else if (!tail1.getRight().isEmpty() && tail2.getRight().isEmpty()) {
                    return 1;
                } else if (tail1.getRight().isEmpty()) {
                    return -1;
                } else {
                    final var c = tail1.getRight().compareTo(tail2.getRight());
                    // compareTo returns numbers outside [-1,1]
                    return c > 0 ? 1 : (c < 0 ? -1 : 0);
                }
            }

            // Making sure Version1 bigger than version2
            if (v1 > v2) {
                return 1;
            }
            // Making sure Version1 smaller than version2
            else if (v1 < v2) {
                return -1;
            }
        }

        // Both are equal
        return 0;
    }

    private static boolean isValidSemver(String check) {
        if (check.length() > 1 && !check.contains(".")) {
            return false;
        }
        String[] stringVals = check.split("\\.");
        for (var i = 0; i < stringVals.length - 1; i++) {
            try {
                Integer.parseInt(stringVals[i]);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        try {
            parseTail(stringVals[stringVals.length - 1]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static Pair<Integer, String> parseTail(String tail) {
        try {
            return Pair.of(Integer.parseInt(tail), "");
        } catch (NumberFormatException e) {
            if (tail.contains("-")) {
                final var splitDash = tail.split("-");
                final var afterDash = String.join("-", Arrays.copyOfRange(splitDash, 1, splitDash.length));
                return Pair.of(Integer.parseInt(splitDash[0]), afterDash);
            } else {
                throw e;
            }
        }
    }

    /***
     * Given a list of FHIR Resources that have the same name, choose the one with the matching
     * version.
     *
     * @param <ResourceType> an IBaseResource type
     * @param resources a list of Resources to select from
     * @param version the version of the Resource to select
     * @param getVersion a function to access version information for the ResourceType
     * @return the Resource with a matching version, or the highest version orwise.
     */
    public static <ResourceType extends IBaseResource> ResourceType selectByVersion(
            List<ResourceType> resources, String version, Function<ResourceType, String> getVersion) {
        checkNotNull(resources);
        checkNotNull(getVersion);

        ResourceType library = null;
        ResourceType maxVersion = null;
        for (ResourceType l : resources) {
            String currentVersion = getVersion.apply(l);
            if (version == null && currentVersion == null || version != null && version.equals(currentVersion)) {
                library = l;
            }

            if (maxVersion == null || compareVersions(currentVersion, getVersion.apply(maxVersion)) >= 0) {
                maxVersion = l;
            }
        }

        // If we were not given a version, return the highest found
        if ((version == null || library == null) && maxVersion != null) {
            return maxVersion;
        }

        return library;
    }
}
