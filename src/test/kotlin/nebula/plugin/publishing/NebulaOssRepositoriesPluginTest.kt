package nebula.plugin.publishing

import nebula.test.dsl.TestKitAssertions.assertThat
import nebula.test.dsl.plugins
import nebula.test.dsl.rootProject
import nebula.test.dsl.testProject
import nebula.test.dsl.version
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class NebulaOssRepositoriesPluginTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `Apply plugin without failures`() {
        val runner = testProject(projectDir) {
            rootProject {
                plugins {
                    id("java")
                    id("com.netflix.nebula.maven-publish") version "19.0.0"
                    id("com.netflix.nebula.oss-publishing")
                }
                rawBuildScript(
                    """
            group = "test"
            nebulaOssPublishing {
                netflixOssRepositoryBaseUrl = "http://"
                netflixOssRepository = "my-releases"
                netflixOssUsername = "user"
                netflixOssPassword = "password"
            }
                """
                )
            }
        }
        val result = runner.run("tasks")
        assertThat(result.output)
            .contains("publishNebulaPublicationToNetflixOSSRepository - Publishes Maven publication 'nebula' to Maven repository 'NetflixOSS'")
    }
}