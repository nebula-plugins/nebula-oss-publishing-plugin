package nebula.plugin.publishing

import nebula.test.dsl.*
import nebula.test.dsl.TestKitAssertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockserver.configuration.ConfigurationProperties
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import java.io.File
import java.net.ServerSocket

internal class NebulaOssPublishingGradleIntegrationTest {
    @TempDir
    lateinit var projectDir: File

    @TempDir
    lateinit var remoteGitDir: File
    private lateinit var artifactory: ClientAndServer
    private var port: Int = 0

    @BeforeEach
    fun startArtifactory() {
        port = try {
            ServerSocket(0).use { socket ->
                socket.getLocalPort()
            }
        } catch (_: Exception) {
            8080
        }
        artifactory = ClientAndServer.startClientAndServer(port)
        ConfigurationProperties.logLevel("ERROR")
    }

    @AfterEach
    fun stopArtifactory() {
        artifactory.stop()
    }

    private fun TestProjectBuilder.sampleSingleProjectSetup() {
        settings {
            name("test")
        }
        rootProject {
            plugins {
                // this plugin has behavior dependent on the existence of the contacts plugin
                id("com.netflix.nebula.contacts") version "latest.release"
                // this plugin has behavior dependent on the existence of the info plugin
                id("com.netflix.nebula.info") version "latest.release"
                id("com.netflix.nebula.maven-publish") version "latest.release"
                // this plugin has behavior dependent on the existence of the release plugin
                id("com.netflix.nebula.release") version "latest.release"
                id("com.netflix.nebula.maven-apache-license") version "latest.release"
                id("com.netflix.nebula.oss-publishing")
                id("com.gradle.plugin-publish") version "2.0.0"
            }
            group("com.netflix")
            gradlePluginSource()
            nebulaOssPublishing("http://localhost:$port")
            //language=kotlin
            rawBuildScript(
                """
description = "description"
contacts {
    addPerson("nebula-plugins-oss@netflix.com") {
        moniker = "Nebula Plugins Maintainers"
        github =  "nebula-plugins"
    }
}
gradlePlugin {
    website = "https://github.com/nebula-plugins/test"
    vcsUrl = "https://github.com/nebula-plugins/test.git"
    plugins {
        create("myPlugin") {
            id = "com.netflix.myplugin"
            displayName = "Test plugin"
            description = "Test description"
            implementationClass = "MyPlugin"
            tags.set(listOf("test"))
        }
    }
}
$MOCK_SIGN
"""
            )
        }
    }

    @Test
    fun `test gradle plugin project candidate`() {
        val version = "0.0.1-rc.1"
        val runner = withGitTag(projectDir, remoteGitDir, "v$version") {
            testProject(projectDir) {
                sampleSingleProjectSetup()
            }
        }
        val verifications = artifactory.expectPublication(
            "netflix-oss",
            "com.netflix",
            "test",
            version
        ) {
            withArtifact("jar")
            withArtifact("sources", "jar")
            withArtifact("javadoc", "jar")
            withGradleModuleMetadata()
        }

        val markerVerifications = artifactory.expectPublication(
            "netflix-oss",
            "com.netflix.myplugin",
            "com.netflix.myplugin.gradle.plugin",
            version
        )

        val result = runner.run(
            "candidate",
            "-Prelease.useLastTag=true",
            "-PnetflixOss.username=user",
            "-PnetflixOss.password=password",
            "--stacktrace"
        )

        assertThat(result.task(":generatePomFileForMyPluginPluginMarkerMavenPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/publications/myPluginPluginMarkerMaven/pom-default.xml"))
            .exists()

        assertThat(result.task(":generateMetadataFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":generatePomFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":verifyNebulaPublicationPomForMavenCentral")).isNull()

        assertThat(result.task(":signPluginMavenPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":signNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":signMyPluginPluginMarkerMavenPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)

        assertThat(result.task(":publishNebulaPublicationToSonatypeRepository")).isNull()
        assertThat(result.task(":publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishPluginMavenPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":publishMyPluginPluginMarkerMavenPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)

        assertThat(result.task(":release")).hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":postRelease")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":candidate")).hasOutcome(TaskOutcome.SUCCESS)

        verifications.verify(artifactory)
        markerVerifications.verify(artifactory)

        assertThat(result.output).doesNotContain("Multiple publications with coordinates")
        val requests = artifactory.retrieveRecordedRequests(HttpRequest.request().withMethod("PUT"))
        assertThat(requests.toSet())
            .`as`("no uploads are repeated")
            .hasSameSizeAs(requests)
    }

    @Test
    fun `test gradle plugin project final`() {
        val version = "0.0.1"
        val runner = withGitTag(projectDir, remoteGitDir, "v$version") {
            testProject(projectDir) {
                sampleSingleProjectSetup()
                rootProject {
                    overrideSonatypeUrl("http://localhost:$port")
                }
            }
        }
        artifactory.mockNexus()
        artifactory.mockGradlePluginPortal("com.netflix.myplugin")
        val netflixOssVerifications = artifactory.expectPublication(
            "netflix-oss",
            "com.netflix",
            "test",
            version
        ) {
            withArtifact("jar")
            withArtifact("sources", "jar")
            withArtifact("javadoc", "jar")
            withGradleModuleMetadata()
        }

        val netflixOssMarkerVerifications = artifactory.expectPublication(
            "netflix-oss",
            "com.netflix.myplugin",
            "com.netflix.myplugin.gradle.plugin",
            version
        )
        val sonatypeVerifications = artifactory.expectPublication(
            "staging/deployByRepositoryId/1",
            "com.netflix",
            "test",
            version
        ) {
            withArtifact("jar")
            withArtifact("sources", "jar")
            withArtifact("javadoc", "jar")
            withGradleModuleMetadata()
        }

        val sonatypeMarkerVerifications = artifactory.expectPublication(
            "staging/deployByRepositoryId/1",
            "com.netflix.myplugin",
            "com.netflix.myplugin.gradle.plugin",
            version
        )
        val result = runner.run(
            "final",
            "publishPlugin", "--validate-only",
            "-Prelease.useLastTag=true",
            "-PnetflixOss.username=user",
            "-PnetflixOss.password=password",
            "-Psonatype.username=user",
            "-Psonatype.password=password",
            "-Dgradle.publish.key=key",
            "-Dgradle.publish.secret=secret",
            "--stacktrace",
            "-Dgradle.portal.url=http://localhost:$port"
        )

        assertThat(result.task(":initializeSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)

        assertThat(result.task(":generateMetadataFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":generatePomFileForNebulaPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":verifyNebulaPublicationPomForMavenCentral"))
            .hasOutcome(TaskOutcome.SUCCESS)

        // gradle plugin signing
        assertThat(result.task(":signMyPluginPluginMarkerMavenPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":signPluginMavenPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)

        // nebula maven central publish
        assertThat(result.task(":publishNebulaPublicationToSonatypeRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishPluginMavenPublicationToSonatypeRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":publishMyPluginPluginMarkerMavenPublicationToSonatypeRepository"))
            .`as` { "plugin marker is published to maven central" }
            .hasOutcome(TaskOutcome.SUCCESS)

        // netflix oss publish
        assertThat(result.task(":publishMyPluginPluginMarkerMavenPublicationToNetflixOSSRepository"))
            .`as` { "plugin marker is published to netflix OSS" }
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishPluginMavenPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SKIPPED)

        assertThat(result.task(":closeSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":releaseSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":closeAndReleaseSonatypeStagingRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishNebulaPublicationToNetflixOSSRepository"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":release")).hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":postRelease")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":final")).hasOutcome(TaskOutcome.SUCCESS)

        assertThat(result.task(":publishPlugins"))
            .`as`("plugin is published to gradle portal")
            .hasOutcome(TaskOutcome.SUCCESS)

        netflixOssVerifications.verify(artifactory)
        netflixOssMarkerVerifications.verify(artifactory)
        sonatypeVerifications.verify(artifactory)
        sonatypeMarkerVerifications.verify(artifactory)

        assertThat(result.output).doesNotContain("Multiple publications with coordinates")
        val requests = artifactory.retrieveRecordedRequests(HttpRequest.request().withMethod("PUT"))
        assertThat(requests.toSet())
            .`as`("no uploads are repeated")
            .hasSameSizeAs(requests)
    }
}