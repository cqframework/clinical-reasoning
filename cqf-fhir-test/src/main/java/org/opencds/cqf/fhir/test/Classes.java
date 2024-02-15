package org.opencds.cqf.fhir.test;

public class Classes {

    private Classes() {
        // intentionally empty
    }

    public static String getResourcePath(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
    }
}
