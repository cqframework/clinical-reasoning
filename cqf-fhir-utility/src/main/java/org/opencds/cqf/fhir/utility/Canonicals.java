package org.opencds.cqf.fhir.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public class Canonicals {

    private static final Pattern LIBRARY_REGEX = Pattern.compile("/Library/");

    private Canonicals() {}

    /**
     * Gets the Resource type component of a canonical url
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Resource type, or null if one can not be parsed
     */
    public static <CanonicalType extends IPrimitiveType<String>> String getResourceType(CanonicalType canonicalType) {
        checkNotNull(canonicalType);
        checkArgument(canonicalType.hasValue());

        return getResourceType(canonicalType.getValue());
    }

    /**
     * Gets the ResourceType component of a canonical url
     *
     * @param canonical the canonical url to parse
     * @return the ResourceType, or null if one can not be parsed
     */
    public static String getResourceType(String canonical) {
        checkNotNull(canonical);

        if (!canonical.contains("/")) {
            return null;
        }

        canonical = canonical.replace(canonical.substring(canonical.lastIndexOf("/")), "");
        return canonical.contains("/") ? canonical.substring(canonical.lastIndexOf("/") + 1) : canonical;
    }

    /**
     * Gets the ID component of a canonical url. Does not include resource name if present in the url.
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Id, or null if one can not be parsed
     */
    public static <CanonicalType extends IPrimitiveType<String>> String getIdPart(CanonicalType canonicalType) {
        checkNotNull(canonicalType);
        checkArgument(canonicalType.hasValue());

        return getIdPart(canonicalType.getValue());
    }

    /**
     * Gets the ID component of a canonical url. Does not include resource name if present in the url.
     *
     * @param canonical the canonical url to parse
     * @return the Id, or null if one can not be parsed
     */
    public static String getIdPart(String canonical) {
        checkNotNull(canonical);

        if (!canonical.contains("/")) {
            return null;
        }

        int lastIndex = calculateLastIndex(canonical);

        return canonical.substring(canonical.lastIndexOf("/") + 1, lastIndex);
    }

    private static String getSystem(String canonical, String resourceType) {
        checkNotNull(canonical);

        if (resourceType == null || resourceType.isBlank()) {
            return null;
        }

        final Pattern resourceTypePattern = "Library".equals(resourceType)
                // Pre-compiled because this is the most common use case
                ? LIBRARY_REGEX
                // In case we get another type of resource
                : Pattern.compile("/%s/".formatted(resourceType)); //

        return getSystem(canonical, resourceTypePattern);
    }

    /**
     * Gets the Version component of a canonical url
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Version, or null if one can not be parsed
     */
    public static <CanonicalType extends IPrimitiveType<String>> String getVersion(CanonicalType canonicalType) {
        checkNotNull(canonicalType);
        checkArgument(canonicalType.hasValue());

        return getVersion(canonicalType.getValue());
    }

    /**
     * Gets the Version component of a canonical url
     *
     * @param canonical the canonical url to parse
     * @return the Version, or null if one can not be parsed
     */
    public static String getVersion(String canonical) {
        checkNotNull(canonical);

        if (!canonical.contains("|")) {
            return null;
        }

        int lastIndex = canonical.lastIndexOf("#");
        if (lastIndex == -1) {
            lastIndex = canonical.length();
        }

        return canonical.substring(canonical.lastIndexOf("|") + 1, lastIndex);
    }

    /**
     * Gets the Url component of a canonical url. Includes the base url, the resource type, and the id
     * if present.
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Url, or null if one can not be parsed
     */
    public static <CanonicalType extends IPrimitiveType<String>> String getUrl(CanonicalType canonicalType) {
        checkNotNull(canonicalType);
        checkArgument(canonicalType.hasValue());

        return getUrl(canonicalType.getValue());
    }

    /**
     * Get the Url component of a canonical url. Includes the base url, the resource type, and the id
     * if present.
     *
     * @param canonical the canonical url to parse
     * @return the Url, or null if one can not be parsed
     */
    public static String getUrl(String canonical) {
        checkNotNull(canonical);

        if (!canonical.contains("/") && !canonical.startsWith("urn:uuid") && !canonical.startsWith("urn:oid")) {
            return null;
        }

        int lastIndex = calculateLastIndex(canonical);

        return canonical.substring(0, lastIndex);
    }

    /**
     * Get the Url component for a set of canonical urls. Includes the base url, the resource type,
     * and the id if present.
     *
     * @param canonicals the set of canonical urls to parse
     * @return the set of Url and null (if one can not be parsed) values
     */
    public static List<String> getUrls(List<String> canonicals) {
        checkNotNull(canonicals);

        List<String> result = new ArrayList<>();
        canonicals.forEach(canonical -> result.add(getUrl(canonical)));

        return result;
    }

    /**
     * Gets the Fragment component of a canonical url.
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Fragment, or null if one can not be parsed
     */
    public static <CanonicalType extends IPrimitiveType<String>> String getFragment(CanonicalType canonicalType) {
        checkNotNull(canonicalType);
        checkArgument(canonicalType.hasValue());

        return getFragment(canonicalType.getValue());
    }

    /**
     * Gets the Fragment component of a canonical url.
     *
     * @param canonical the canonical url to parse
     * @return the Fragment, or null if one can not be parsed
     */
    public static String getFragment(String canonical) {
        checkNotNull(canonical);

        if (!canonical.contains("#")) {
            return null;
        }

        return canonical.substring(canonical.lastIndexOf("#") + 1);
    }

    public static <CanonicalType extends IPrimitiveType<String>> CanonicalParts getParts(CanonicalType canonicalType) {
        checkNotNull(canonicalType);
        checkArgument(canonicalType.hasValue());

        return getParts(canonicalType.getValue());
    }

    public static CanonicalParts getParts(String canonical) {
        checkNotNull(canonical);

        String url = getUrl(canonical);
        String id = getIdPart(canonical);
        String resourceType = getResourceType(canonical);
        String system = getSystem(canonical, resourceType);
        String version = getVersion(canonical);
        String fragment = getFragment(canonical);
        return new CanonicalParts(url, system, id, resourceType, version, fragment);
    }

    private static int calculateLastIndex(String canonical) {
        int lastIndexOfBar = canonical.lastIndexOf("|");
        int lastIndexOfHash = canonical.lastIndexOf("#");

        int lastIndex = canonical.length();
        int mul = lastIndexOfBar * lastIndexOfHash;
        if (mul > 1) {
            lastIndex = Math.min(lastIndexOfBar, lastIndexOfHash);
        } else if (mul < 0) {
            lastIndex = Math.max(lastIndexOfBar, lastIndexOfHash);
        }
        return lastIndex;
    }

    private static String getSystem(String canonical, Pattern resourceTypePattern) {
        checkNotNull(canonical);

        if (resourceTypePattern == null) {
            return null;
        }

        if (!resourceTypePattern.matcher(canonical).find()) {
            return null;
        }

        return resourceTypePattern.split(canonical)[0];
    }

    public static final class CanonicalParts {
        private final String url;
        private final String system;
        private final String idPart;
        private final String resourceType;
        private final String version;
        private final String fragment;

        CanonicalParts(String url, String system, String idPart, String resourceType, String version, String fragment) {
            this.url = url;
            this.system = system;
            this.idPart = idPart;
            this.resourceType = resourceType;
            this.version = version;
            this.fragment = fragment;
        }

        public String url() {
            return this.url;
        }

        public String system() {
            return this.system;
        }

        public String idPart() {
            return this.idPart;
        }

        public String resourceType() {
            return this.resourceType;
        }

        public String version() {
            return this.version;
        }

        public String fragment() {
            return this.fragment;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CanonicalParts that = (CanonicalParts) o;
            return Objects.equals(url, that.url)
                    && Objects.equals(system, that.system)
                    && Objects.equals(idPart, that.idPart)
                    && Objects.equals(resourceType, that.resourceType)
                    && Objects.equals(version, that.version)
                    && Objects.equals(fragment, that.fragment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, system, idPart, resourceType, version, fragment);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CanonicalParts.class.getSimpleName() + "[", "]")
                    .add("url='" + url + "'")
                    .add("system='" + system + "'")
                    .add("idPart='" + idPart + "'")
                    .add("resourceType='" + resourceType + "'")
                    .add("version='" + version + "'")
                    .add("fragment='" + fragment + "'")
                    .toString();
        }
    }
}
