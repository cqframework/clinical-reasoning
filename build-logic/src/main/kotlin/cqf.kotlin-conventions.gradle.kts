plugins {
    id("cqf.java-conventions")
    kotlin("jvm")
}

kotlin {
    // Align the Kotlin JVM target with the Java toolchain already configured
    // by cqf.java-conventions. Using jvmToolchain here also sets the JVM
    // target for the Kotlin compiler so the two don't diverge.
    jvmToolchain(17)

    // Allow .kt files to live alongside .java files in src/main/java and
    // src/test/java. The Kotlin compiler picks up .kt files from these dirs;
    // the Java compiler independently processes .java files there as before.
    sourceSets {
        main {
            kotlin.srcDir("src/main/java")
        }
        test {
            kotlin.srcDir("src/test/java")
        }
    }
}
