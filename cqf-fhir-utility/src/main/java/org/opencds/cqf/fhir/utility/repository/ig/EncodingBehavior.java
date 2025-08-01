package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.rest.api.EncodingEnum;
import java.util.EnumSet;

/**
 * This class is used to determine how to handle encoding when reading and writing resources. You can
 * choose to preserve the encoding of the resource when reading and writing, or you can choose to change
 * the encoding to a preferred encoding when writing. New resources will always be written in the preferred
 * encoding.
 */
public record EncodingBehavior(
        EncodingEnum preferredEncoding, PreserveEncoding preserveEncoding, EnumSet<EncodingEnum> enabledEncodings) {

    /**
     * When updating a resource, you can choose to preserve the original encoding of the resource
     * or you can choose to overwrite the original encoding with the preferred encoding.
     */
    public enum PreserveEncoding {
        PRESERVE_ORIGINAL_ENCODING,
        OVERWRITE_WITH_PREFERRED_ENCODING
    }

    private static final EnumSet<EncodingEnum> DEFAULT_ENABLED_ENCODINGS =
            EnumSet.of(EncodingEnum.JSON, EncodingEnum.XML);

    public static final EncodingBehavior DEFAULT = new EncodingBehavior(
            EncodingEnum.JSON, PreserveEncoding.PRESERVE_ORIGINAL_ENCODING, DEFAULT_ENABLED_ENCODINGS);

    public static final EncodingBehavior KALM = new EncodingBehavior(
            EncodingEnum.JSON, PreserveEncoding.PRESERVE_ORIGINAL_ENCODING, EnumSet.of(EncodingEnum.JSON));

    public EncodingBehavior(EncodingEnum preferredEncoding, PreserveEncoding preserveEncoding) {
        this(preferredEncoding, preserveEncoding, DEFAULT_ENABLED_ENCODINGS);
    }
}
