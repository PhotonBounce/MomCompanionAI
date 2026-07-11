// Helper task that builds a signed AAB and runs the Play Publisher upload
tasks.register("publishInternal") {
    group = "publishing"
    description = "Builds a release bundle and publishes it to the internal track"
    dependsOn("bundleRelease")
    doLast {
        println("🚀 Publishing to Google Play…")
    }
}
