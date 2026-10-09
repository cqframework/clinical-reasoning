plugins { alias(libs.plugins.animalsniffer) }

dependencies {
    "signature"(
        "com.toasttab.android:gummy-bears-api-34:${libs.versions.gummy.bears.get()}@signature"
    )
}
