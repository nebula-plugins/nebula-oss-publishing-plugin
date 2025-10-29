package nebula.plugin.publishing

import nebula.test.dsl.TestKitAssertions.assertThat
import nebula.test.dsl.plugins
import nebula.test.dsl.rootProject
import nebula.test.dsl.testProject
import nebula.test.dsl.version
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class NebulaOssPublishingPluginTest {
    @TempDir
    lateinit var projectDir: File

    @TempDir
    lateinit var remoteGitDir: File

    @Test
    fun `test single project final`() {
        val runner = withGitTag(projectDir, remoteGitDir, "v0.0.1") {
            testProject(projectDir) {
                rootProject {
                    plugins {
                        id("java")
                        // this plugin has behavior dependent on the existence of the contacts plugin
                        id("com.netflix.nebula.contacts") version "latest.release"
                        // this plugin has behavior dependent on the existence of the info plugin
                        id("com.netflix.nebula.info") version "latest.release"
                        id("com.netflix.nebula.maven-publish") version "latest.release"
                        // this plugin has behavior dependent on the existence of the release plugin
                        id("com.netflix.nebula.release") version "latest.release"
                        id("com.netflix.nebula.maven-apache-license") version "latest.release"
                        id("com.netflix.nebula.oss-publishing")
                    }
                    //language=kotlin
                    rawBuildScript(
                        """
group = "test"
description = "description"
nebulaOssPublishing {
    packageGroup = "test"
    netflixOssRepositoryBaseUrl = "http://"
    netflixOssRepository = "my-releases"
}
contacts {
    addPerson("nebula-plugins-oss@netflix.com") {
        moniker = "Nebula Plugins Maintainers"
        github =  "nebula-plugins"
    }
}
$DISABLE_PUBLISH_TASKS
$DISABLE_MAVEN_CENTRAL_TASKS
"""
                    )
                }
            }
        }
        val result = runner.run(
            "final",
            "-Prelease.useLastTag=true",
            "-PnetflixOss.username=user",
            "-PnetflixOss.password=password",
            "-Psonatype.username=user",
            "-Psonatype.password=password",
            "--stacktrace"
        )
        assertThat(result.task(":initializeSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":generateMetadataFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":generatePomFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":verifyNebulaPublicationPomForMavenCentral"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishNebulaPublicationToSonatypeRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":closeSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":releaseSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":closeAndReleaseSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.UP_TO_DATE)
        assertThat(result.task(":publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":release")).hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":postRelease")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":final")).hasOutcome(TaskOutcome.SUCCESS)
    }
}