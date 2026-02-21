plugins {
    jacoco
    java
}

jacoco {
    toolVersion = BuildConfig.JACOCO
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    dependsOn(tasks.named("classes"))
    violationRules {
        isFailOnViolation = false
        rule {
            element = "CLASS"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
            excludes = listOf(
                "**/*Provider.*",
                "**/config/**/*"
            )
        }
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/*Provider.class",
                    "**/config/**/*.class",
                    "**/*Exception.class",
                    "**/Benchmarks.class",
                    "**/*Constants.class",
                    "**/*Constants\$*.class",
                    "**/*Settings.class",
                    "**/argument/*Argument.class",
                    "**/*Type.class",
                    "**/*Def.class",
                    "**/ClinicalIntelligenceBundleProviderUtil.class",
                    "**/ClinicalIntelligenceBundleProviderUtil\$*.class",
                    "**/R4ResourceIdUtils.class",
                    "**/CRMIReleaseVersionBehavior.class",
                    "**/CRMIReleaseVersionBehavior\$*.class",
                    "**/CRMIReleaseExperimentalBehavior.class",
                    "**/CRMIReleaseExperimentalBehavior\$*.class",
                    "**/TransformProperties.class",
                    "**/ArtifactAssessment.class",
                    "**/ArtifactAssessment\$*.class",
                    "**/SearchHelper.class",
                    "**/PackageHelper.class",
                    "**/ContainedHelper.class",
                    "**/MetadataResourceHelper.class",
                    "**/ArtifactDiffProcessor.class",
                    "**/ArtifactDiffProcessor\$*.class",
                    "**/DiffCacheResource.class",
                    "**/Dstu3MeasureService.class",
                    "**/Dstu3MeasureReportBuilder.class",
                    "**/Dstu3MeasureReportBuilder\$*.class",
                    "**/Dstu3MeasureProcessor.class",
                    "**/MedicationRequestResolver.class",
                    "**/StratumValueWrapper.class",
                    "**/LibraryInitHandler.class",
                    "**/LibraryEngine.class",
                    "**/CqlExecutionProcessor.class",
                    "**/GraphDefinitionProcessor.class",
                    "**/ApplyRequest.class",
                    "**/ApplyRequestBuilder.class",
                    "**/ResponseBundle.class",
                    "**/InferManifestParametersVisitor.class",
                    "**/LibraryVersionSelector.class",
                    "**/CqlEngineOptions.class",
                    "**/CodeExtractor.class",
                    "**/DynamicModelResolver.class",
                    "**/EngineFactory.class",
                    "**/R4PackageService.class",
                    "**/R4DraftService.class",
                    "**/R4ApproveService.class",
                    "**/R4InferManifestParametersService.class",
                    "**/KnowledgeArtifactProcessor.class",
                    "**/R4ReleaseService.class",
                    "**/ReleaseProcessor.class",
                    "**/ImplementationGuideProcessor.class",
                    "**/ImplementationGuideAdapter.class",
                    "**/TaskResolver.class",
                    "**/CommunicationResolver.class",
                    "**/ServiceRequestResolver.class",
                    "**/ValueWrapperUtil\$*.class",
                    "**/AdditionalRequestHeadersInterceptor.class",
                    "**/IQuestionnaireAdapter.class",
                    "**/IKnowledgeArtifactAdapter.class",
                    "**/IParametersParameterComponentAdapter.class",
                    "**/IStructureDefinitionAdapter.class",
                    "**/BundleIterator.class",
                    "**/BundleIterable.class",
                    "**/BundleMappingIterator.class",
                    "**/StreamIterable.class",
                    "**/AttachmentAdapter.class",
                    "**FederatedRepository.class",
                    "**/ProxyRepository.class",
                    "**/Repositories.class",
                    "**/RestRepository.class",
                    "**/FederatedRepository.class",
                    "**/ResourceLoader.class",
                    "**/FhirResourceLoader.class",
                    "**/OperationParametersParser.class",
                    "**/FhirVersions.class",
                    "**/Parameters.class",
                    "**/VersionUtilities.class",
                    "**/ValidationProfile.class",
                    "**/BundleHelper.class",
                    "**/ResourceValidator.class",
                    "**/CqfApplicabilityBehavior.class",
                    "**/ResourceMatcher\$*.class",
                    "**/ResourceMatcher*.class",
                    "**/BaseResourceBuilder.class",
                    "**/BaseBackboneElementBuilder.class",
                    "**/DetectedIssueBuilder.class",
                    "**/BaseDomainResourceBuilder.class",
                    "**/CompositionSectionComponentBuilder.class",
                    "**/CompositionBuilder.class",
                    "**/BundleBuilder.class",
                    "**/Eithers.class",
                    "**/Tries.class",
                    "**/Searches.class",
                    "**/IdCreator.class",
                    "**/ResourceCreator.class",
                    "**/ValueSetAdapter.class",
                    "**/Main.class",
                    "**/Resources.class",
                    "**/ICdsCrService.class",
                    "**/HeaderInjectionInterceptor.class",
                    "**/BundleMappingIterable.class",
                    "**/StratifierRowKey.class"
                )
            }
        })
    )
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
