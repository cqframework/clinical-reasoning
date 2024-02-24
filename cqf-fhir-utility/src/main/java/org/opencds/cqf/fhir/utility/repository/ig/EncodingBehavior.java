package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.rest.api.EncodingEnum;

/**
 * This class is used to determine how to handle encoding when reading and writing resources. You can
 * choose to preserve the encoding of the resource when reading and writing, or you can choose to change
 * the encoding to a preferred encoding when writing. New resources will always be written in the preferred
 * encoding.
 */
public class EncodingBehavior {

    public static final EncodingBehavior DEFAULT =
            new EncodingBehavior(EncodingEnum.JSON, EncodingPreservationMode.PRESERVE);

    private final EncodingEnum preferredEncoding;
    private final EncodingPreservationMode preservationMode;

    public EncodingBehavior(EncodingEnum preferredEncoding, EncodingPreservationMode encodingPreservation) {
        this.preferredEncoding = preferredEncoding;
        this.preservationMode = encodingPreservation;
    }

    EncodingEnum preferredEncoding() {
        return preferredEncoding;
    }

    EncodingPreservationMode preservationMode() {
        return preservationMode;
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
        return preservationMode == EncodingPreservationMode.PRESERVE ? sourceEncoding : this.preferredEncoding;
    }
}
