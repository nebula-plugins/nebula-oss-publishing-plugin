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
            rootProject {
                plugins {
                    id("com.netflix.nebula.maven-publish") version ("19.0.0")
                    java()
                    id("com.netflix.nebula.signing")
                }
                rawBuildScript(
                    """
nebulaOssPublishing {
    signingKey = "something"
    signingPassword = "something"
}
tasks.named("publishNebulaPublicationToMavenLocal") { onlyIf { false }}
afterEvaluate {
    tasks.named("signNebulaPublication") { onlyIf { false }}
}
"""
                )
            }
        }

        val result = runner.run("publishNebulaPublicationToMavenLocal")
        assertThat(result.task(":signNebulaPublication")).hasOutcome(TaskOutcome.SKIPPED)
    }

    @Test
    fun `do not register signing task if key and password are not present`() {
        val runner = testProject(projectDir) {
            rootProject {
                plugins {
                    id("com.netflix.nebula.maven-publish") version ("19.0.0")
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
                    id("com.netflix.nebula.maven-publish") version ("19.0.0")
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
tasks.named("publishCustomMavenPublicationToMavenLocal") { onlyIf { false }}
tasks.named("publishNebulaPublicationToMavenLocal") { onlyIf { false }}
afterEvaluate {
    tasks.named("signNebulaPublication") { onlyIf { false }}
}
"""
                )
            }
        }

        val result =
            runner.run("publishCustomMavenPublicationToMavenLocal", "publishNebulaPublicationToMavenLocal")

        assertThat(result.task(":signNebulaPublication")).hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":signCustomMavenPublication")).isNull()
    }

    @Test
    fun `gradle plugin signs nebula publication and corresponding plugin marker`() {
        val runner = testProject(projectDir) {
            rootProject {
                plugins {
                    id("com.netflix.nebula.maven-publish") version ("19.0.0")
                    id("java-gradle-plugin")
                    id("com.netflix.nebula.signing")
                }
                src {
                    main {
                        java("MyPlugin.java","""
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MyPlugin implements Plugin<Project> {
    public void apply(Project project){
    }
}
""")
                    }
                }
                rawBuildScript(
                    //language=kotlin
                    """
nebulaOssPublishing {
    signingKey = "something"
    signingPassword = "something"
}
gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.netflix.test"
            displayName = "Test plugin"
            description = "Test description"
            implementationClass = "MyPlugin"
        }
    }
}
tasks.named("publishNebulaPublicationToMavenLocal") { onlyIf { false }}
afterEvaluate {
    tasks.named("signNebulaPublication") { onlyIf { false }}
    //tasks.named("signMyPluginMarkerMavenPublication") { onlyIf { false }}
}
"""
                )
            }
        }

        val result = runner.run("publishNebulaPublicationToMavenLocal")
projectDir.resolve("build/publications/myPluginPluginMarkerMaven")
        assertThat(result.task(":signNebulaPublication")).hasOutcome(TaskOutcome.SKIPPED)
        assertThat(result.task(":signMyPluginMarkerMavenPublication")).hasOutcome(TaskOutcome.SKIPPED)
    }
}
