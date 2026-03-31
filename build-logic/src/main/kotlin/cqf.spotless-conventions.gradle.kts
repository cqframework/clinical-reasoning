plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        palantirJavaFormat(BuildConfig.PALANTIR_FORMAT)
    }
    kotlin {
        ktfmt(BuildConfig.KTFMT).kotlinlangStyle()
    }

    kotlinGradle {
        ktfmt(BuildConfig.KTFMT).kotlinlangStyle()
    }
}
