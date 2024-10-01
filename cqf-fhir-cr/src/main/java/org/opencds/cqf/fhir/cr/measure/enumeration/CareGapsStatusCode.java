package org.opencds.cqf.fhir.cr.measure.enumeration;

import ca.uhn.fhir.i18n.Msg;

public enum CareGapsStatusCode {
    OPEN_GAP("open-gap"),
    CLOSED_GAP("closed-gap"),
    NOT_APPLICABLE("not-applicable"),
    PROSPECTIVE_GAP("prospective-gap");

    private final String value;

    CareGapsStatusCode(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public String toDisplayString() {
        if (value.equals("open-gap")) {
            return "Open Gap";
        }

        if (value.equals("closed-gap")) {
            return "Closed Gap";
        }

        if (value.equals("not-applicable")) {
            return "Not Applicable";
        }

        if (value.equals("prospective-gap")) {
            return "Prospective Gap";
        }

        throw new RuntimeException(Msg.code(2301) + "Error getting display strings for care gaps status codes");
    }
}
