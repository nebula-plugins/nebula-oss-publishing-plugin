/**
 *
 *  Copyright 2021 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package nebula.plugin.publishing

import nebula.test.dsl.TestKitAssertions.assertThat
import nebula.test.dsl.*
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class NebulaSigningPluginTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `register signing task`() {
        val runner = testProject(projectDir) {
            settings{
                name("test")
            }
            rootProject {
                plugins {
                    id("com.netflix.nebula.maven-publish") version ("latest.release")
                    java()
                    id("com.netflix.nebula.signing")
                }
                rawBuildScript(
                    """
nebulaOssPublishing {
    signingKey = "something"
    signingPassword = "something"
}
$DISABLE_PUBLISH_TASKS
$MOCK_SIGN
"""
                )
            }
        }

        val result = runner.run("publishNebulaPublicationToMavenLocal")
        assertThat(result.task(":signNebulaPublication")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/libs/test.jar")).exists()
        assertThat(projectDir.resolve("build/libs/test.jar.asc")).exists()
        assertThat(projectDir.resolve("build/publications/nebula/pom-default.xml")).exists()
        assertThat(projectDir.resolve("build/publications/nebula/pom-default.xml.asc")).exists()
        assertThat(projectDir.resolve("build/publications/nebula/module.json")).exists()
        assertThat(projectDir.resolve("build/publications/nebula/module.json.asc")).exists()
    }

    @Test
    fun `do not register signing task if key and password are not present`() {
        val runner = testProject(projectDir) {
            rootProject {
                plugins {
                    id("com.netflix.nebula.maven-publish") version ("latest.release")
                    java()
                    id("com.netflix.nebula.signing")
                }
            }
        }

        val result = runner.run("publishNebulaPublicationToMavenLocal", "--dry-run")
        assertThat(result.task(":signNebulaPublication")).isNull()
    }

    @Test
    fun `only signs nebula publication`() {
        val runner = testProject(projectDir) {
            rootProject {
                plugins {
                    id("com.netflix.nebula.maven-publish") version ("latest.release")
                    java()
                    id("com.netflix.nebula.signing")
                }
                rawBuildScript(
                    """
nebulaOssPublishing {
    signingKey = "something"
    signingPassword = "something"
}
publishing {
    publications {
        create<MavenPublication>("customMaven") {
            groupId = "org.gradle.sample"
            artifactId = "library"
            version = "1.1"

            from(components["java"])
        }
    }
}
$DISABLE_PUBLISH_TASKS
$MOCK_SIGN
"""
                )
            }
        }

        val result =
            runner.run("publishCustomMavenPublicationToMavenLocal", "publishNebulaPublicationToMavenLocal")

        assertThat(result.task(":signNebulaPublication")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":signCustomMavenPublication")).isNull()
    }

    @Test
    fun `gradle plugin signs plugin publication and corresponding plugin marker`() {
        val runner = testProject(projectDir) {
            settings {
                name("test")
            }
            rootProject {
                plugins {
                    id("com.netflix.nebula.maven-publish") version ("latest.release")
                    id("com.netflix.nebula.signing")
                    id("com.gradle.plugin-publish") version "2.0.0"
                }
                group("com.netflix")
                gradlePluginSource()
                rawBuildScript(
                    //language=kotlin
                    """
nebulaOssPublishing {
    signingKey = "something"
    signingPassword = "something"
}
gradlePlugin {
    website = "https://github.com/nebula-plugins/test"
    vcsUrl = "https://github.com/nebula-plugins/test.git"
    plugins {
        create("myPlugin") {
            id = "com.netflix.test"
            displayName = "Test plugin"
            description = "Test description"
            implementationClass = "MyPlugin"
            tags.set(listOf("test"))
        }
    }
}
$DISABLE_PUBLISH_TASKS
$MOCK_SIGN
"""
                )
            }
        }

        val result = runner.run(
            "publishPlugins", "--validate-only",
            "-Pgradle.publish.key=key",
            "-Pgradle.publish.secret=secret",
            "-Pversion=0.0.1"
        )
        projectDir.resolve("build/publications/pluginMaven").listFiles().forEach {
            file ->println(file.toRelativeString(projectDir))
        }
        projectDir.resolve("build/publications/myPluginPluginMarkerMaven").listFiles().forEach {
                file ->println(file.toRelativeString(projectDir))
        }

        assertThat(result.task(":signMyPluginPluginMarkerMavenPublication"))
            .hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":signPluginMavenPublication")).hasOutcome(TaskOutcome.SUCCESS)
        assertThat(result.task(":publishPlugins")).hasOutcome(TaskOutcome.SKIPPED)

        assertThat(projectDir.resolve("build/libs/test-0.0.1.jar")).exists()
        assertThat(projectDir.resolve("build/libs/test-0.0.1.jar.asc")).exists()
        assertThat(projectDir.resolve("build/libs/test-0.0.1-sources.jar")).exists()
        assertThat(projectDir.resolve("build/libs/test-0.0.1-sources.jar.asc")).exists()
        assertThat(projectDir.resolve("build/libs/test-0.0.1-javadoc.jar")).exists()
        assertThat(projectDir.resolve("build/libs/test-0.0.1-javadoc.jar.asc")).exists()

        assertThat(projectDir.resolve("build/publications/pluginMaven/pom-default.xml")).exists()
        assertThat(projectDir.resolve("build/publications/pluginMaven/pom-default.xml.asc")).exists()
        assertThat(projectDir.resolve("build/publications/pluginMaven/module.json")).exists()
        assertThat(projectDir.resolve("build/publications/pluginMaven/module.json.asc")).exists()

        assertThat(projectDir.resolve("build/publications/myPluginPluginMarkerMaven/pom-default.xml")).exists()
        assertThat(projectDir.resolve("build/publications/myPluginPluginMarkerMaven/pom-default.xml.asc")).exists()
    }
}
