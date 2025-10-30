package nebula.plugin.publishing

import nebula.test.dsl.*
import nebula.test.dsl.TestKitAssertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class MultiprojectIntegrationTest {
    @TempDir
    lateinit var projectDir: File

    @TempDir
    lateinit var remoteGitDir: File

    private fun TestProjectBuilder.sampleMultiProjectSetup() {
        settings {
            pluginManagement {
                plugins {
                    id("com.netflix.nebula.contacts") version "latest.release"
                    id("com.netflix.nebula.maven-publish") version "22.1.0"
                    id("com.netflix.nebula.maven-apache-license") version "22.1.0"
                    id("com.netflix.nebula.info") version "latest.release"
                    id("com.netflix.nebula.release") version "latest.release"
                }
            }
        }
        rootProject {
            plugins {
                // this plugin has behavior dependent on the existence of the contacts plugin
                id("com.netflix.nebula.contacts")
                // this plugin has behavior dependent on the existence of the info plugin
                id("com.netflix.nebula.info")
                // this plugin has behavior dependent on the existence of the release plugin
                id("com.netflix.nebula.release")
                id("com.netflix.nebula.oss-publishing")
            }
            //language=kotlin
            rawBuildScript(
                """

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
$DISABLE_MAVEN_CENTRAL_TASKS
"""
            )
        }
        subProject("sub1") {
            plugins {
                id("java")
                // this plugin has behavior dependent on the existence of the contacts plugin
                id("com.netflix.nebula.contacts")
                // this plugin has behavior dependent on the existence of the info plugin
                id("com.netflix.nebula.info")
                id("com.netflix.nebula.maven-publish")
                id("com.netflix.nebula.maven-apache-license")
                id("com.netflix.nebula.oss-publishing")
                id("com.netflix.nebula.release")
            }
            //language=kotlin
            rawBuildScript(
                """
group = "test"
description = "description"
$DISABLE_PUBLISH_TASKS
$MOCK_SIGN
"""
            )
        }
        subProject("sub2") {
            plugins {
                id("java")
                // this plugin has behavior dependent on the existence of the contacts plugin
                id("com.netflix.nebula.contacts")
                // this plugin has behavior dependent on the existence of the info plugin
                id("com.netflix.nebula.info")
                id("com.netflix.nebula.maven-publish")
                id("com.netflix.nebula.maven-apache-license")
                id("com.netflix.nebula.oss-publishing")
                id("com.netflix.nebula.release")
            }
            //language=kotlin
            rawBuildScript(
                """
group = "test"
description = "description"
$DISABLE_PUBLISH_TASKS
$MOCK_SIGN
"""
            )
        }
    }

    @Test
    fun `test multi project candidate`() {
        val runner = withGitTag(projectDir, remoteGitDir, "v0.0.1-rc.1") {
            testProject(projectDir) {
                sampleMultiProjectSetup()
            }
        }
        val result = runner.run(
            "candidate",
            "-Prelease.useLastTag=true",
            "-PnetflixOss.username=user",
            "-PnetflixOss.password=password",
            "--stacktrace"
        )
        assertThat(result.task(":initializeSonatypeStagingRepository")).isNull()

        // sub1
        assertThat(result.task(":sub1:generateMetadataFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub1:generatePomFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub1:verifyNebulaPublicationPomForMavenCentral")).isNull()
        assertThat(result.task(":sub1:publishNebulaPublicationToSonatypeRepository")).isNull()
        assertThat(result.task(":sub1:publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)

        // sub2
        assertThat(result.task(":sub2:generateMetadataFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub2:generatePomFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub2:verifyNebulaPublicationPomForMavenCentral")).isNull()
        assertThat(result.task(":sub2:publishNebulaPublicationToSonatypeRepository")).isNull()
        assertThat(result.task(":sub2:publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)

        assertThat(result.task(":closeSonatypeStagingRepository")).isNull()
        assertThat(result.task(":releaseSonatypeStagingRepository")).isNull()
        assertThat(result.task(":closeAndReleaseSonatypeStagingRepository")).isNull()

        assertThat(result.task(":release")).hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":postRelease")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":candidate")).hasOutcome(TaskOutcome.SUCCESS)
    }

    @Test
    fun `test multi project final`() {
        val runner = withGitTag(projectDir, remoteGitDir, "v0.0.1") {
            testProject(projectDir) {
                sampleMultiProjectSetup()
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

        // sub1
        assertThat(result.task(":sub1:generateMetadataFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub1:generatePomFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub1:verifyNebulaPublicationPomForMavenCentral"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub1:publishNebulaPublicationToSonatypeRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":sub1:publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)

        // sub2
        assertThat(result.task(":sub2:generateMetadataFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub2:generatePomFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub2:verifyNebulaPublicationPomForMavenCentral"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":sub2:publishNebulaPublicationToSonatypeRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":sub2:publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)

        assertThat(result.task(":closeSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":releaseSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":closeAndReleaseSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.UP_TO_DATE)

        assertThat(result.task(":release")).hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":postRelease")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":final")).hasOutcome(TaskOutcome.SUCCESS)
    }
}