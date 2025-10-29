package nebula.plugin.publishing

import nebula.test.dsl.TestKitAssertions.assertThat
import nebula.test.dsl.pluginManagement
import nebula.test.dsl.plugins
import nebula.test.dsl.rootProject
import nebula.test.dsl.settings
import nebula.test.dsl.subProject
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

    companion object {
        //language=kotlin
        const val DISABLE_PUBLISH_TASKS : String = """
afterEvaluate {
    tasks.withType<AbstractPublishToMaven>() {
        onlyIf { false }
    }
    tasks.withType<Sign>(){
        onlyIf { false } // we don't have a signing key in integration tests (yet)
    }
}
"""


    //language=kotlin
    const val DISABLE_MAVEN_CENTRAL_TASKS : String = """
tasks.named("initializeSonatypeStagingRepository"){
    onlyIf { false }
}
tasks.named("closeSonatypeStagingRepository"){
    onlyIf { false }
}
tasks.named("releaseSonatypeStagingRepository"){
    onlyIf { false }
}
"""
}

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

    @Test
    fun `test multi project final`() {
        val runner = withGitTag(projectDir, remoteGitDir, "v0.0.1") {
            testProject(projectDir) {
                settings {
                    pluginManagement {
                        plugins {
                            id("com.netflix.nebula.contacts") version "latest.release"
                            id("com.netflix.nebula.maven-publish") version "22.1.0"
                            id("com.netflix.nebula.maven-apache-license") version "22.1.0"
                            id("com.netflix.nebula.info") version "latest.release"
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
                        id("com.netflix.nebula.release") version "latest.release"
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
                subProject("sub1"){
                    plugins {
                        id("java")
                        // this plugin has behavior dependent on the existence of the contacts plugin
                        id("com.netflix.nebula.contacts")
                        // this plugin has behavior dependent on the existence of the info plugin
                        id("com.netflix.nebula.info")
                        id("com.netflix.nebula.maven-publish")
                        id("com.netflix.nebula.maven-apache-license")
                        id("com.netflix.nebula.oss-publishing")
                        id("com.netflix.nebula.release") version "latest.release"
                    }
                    //language=kotlin
                    rawBuildScript("""
group = "test"
description = "description"
$DISABLE_PUBLISH_TASKS
""")
                }
                subProject("sub2"){
                    plugins {
                        id("java")
                        // this plugin has behavior dependent on the existence of the contacts plugin
                        id("com.netflix.nebula.contacts")
                        // this plugin has behavior dependent on the existence of the info plugin
                        id("com.netflix.nebula.info")
                        id("com.netflix.nebula.maven-publish")
                        id("com.netflix.nebula.maven-apache-license")
                        id("com.netflix.nebula.oss-publishing")
                        id("com.netflix.nebula.release") version "latest.release"
                    }
                    //language=kotlin
                    rawBuildScript("""
group = "test"
description = "description"
$DISABLE_PUBLISH_TASKS
""")
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