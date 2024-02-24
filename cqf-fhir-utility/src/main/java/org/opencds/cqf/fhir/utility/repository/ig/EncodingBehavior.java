package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.rest.api.EncodingEnum;

/**
 * This class is used to determine how to handle encoding when reading and writing resources. You can
 * choose to preserve the encoding of the resource when reading and writing, or you can choose to change
 * the encoding to a preferred encoding when writing. New resources will always be written in the preferred
 * encoding.
 */
public class EncodingBehavior {

    /**
     * When updating a resource, you can choose to preserve the original encoding of the resource
     * or you can choose to overwrite the original encoding with the preferred encoding.
     */
    public enum PreserveEncoding {
        PRESERVE_ORIGINAL_ENCODING,
        OVERWRITE_WITH_PREFERRED_ENCODING
    }

    public static final EncodingBehavior DEFAULT =
            new EncodingBehavior(EncodingEnum.JSON, PreserveEncoding.PRESERVE_ORIGINAL_ENCODING);

    private final EncodingEnum preferredEncoding;
    private final PreserveEncoding preserveEncoding;

    public EncodingBehavior(EncodingEnum preferredEncoding, PreserveEncoding preserveEncoding) {
        this.preferredEncoding = preferredEncoding;
        this.preserveEncoding = preserveEncoding;
    }

    EncodingEnum preferredEncoding() {
        return preferredEncoding;
    }

    PreserveEncoding preserveEncoding() {
        return preserveEncoding;
    }

    /**
     * This method will return the encoding that should be used when writing a resource. If the sourceEncoding
     * is null, then the preferredEncoding will be returned. If the preservationMode is set to PRESERVE, then the
     * sourceEncoding will be returned. If the preservationMode is set to OVERWRITE, then the preferredEncoding will
     * be returned.
     *
     * @param sourceEncoding the original encoding of the resource
     * @return the encoding that should be used when writing the resource
     */
    EncodingEnum encodingFor(EncodingEnum sourceEncoding) {
        if (sourceEncoding == null) {
            return this.preferredEncoding;
        }
        return preserveEncoding == PreserveEncoding.PRESERVE_ORIGINAL_ENCODING
                ? sourceEncoding
                : this.preferredEncoding;
    }
}
