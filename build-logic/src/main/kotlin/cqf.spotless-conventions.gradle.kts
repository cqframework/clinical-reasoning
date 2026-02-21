plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        palantirJavaFormat(BuildConfig.PALANTIR_FORMAT)
    }
}
