/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.publishing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.configure
import org.gradle.util.GradleVersion

/**
 * Responsible for configuring a repository for maven publications that will go to NetflixOSS repositories
 */
class NebulaOssRepositoriesPlugin : Plugin<Project> {
    companion object {
        const val netflixOssRepoIdentifier = "NetflixOSS"
    }

    override fun apply(project: Project) {
        val nebulaOssPublishingExtension = project.rootProject.extensions.findByType(NebulaOssPublishingExtension::class.java) ?: project.rootProject.extensions.create("nebulaOssPublishing", NebulaOssPublishingExtension::class.java)
        project.plugins.withId("nebula.maven-publish") {
            project.afterEvaluate {
                if(!nebulaOssPublishingExtension.netflixOssRepositoryBaseUrl.isPresent
                    || !nebulaOssPublishingExtension.netflixOssRepository.isPresent
                    || !nebulaOssPublishingExtension.netflixOssUsername.isPresent
                    || !nebulaOssPublishingExtension.netflixOssPassword.isPresent) {
                    return@afterEvaluate
                }
                project.extensions.configure<PublishingExtension> {
                    repositories {
                        maven {
                            name = netflixOssRepoIdentifier
                            url = project.uri("${nebulaOssPublishingExtension.netflixOssRepositoryBaseUrl.get()}/${nebulaOssPublishingExtension.netflixOssRepository.get()}")
                            credentials {
                                username = nebulaOssPublishingExtension.netflixOssUsername.get()
                                password = nebulaOssPublishingExtension.netflixOssPassword.get()
                            }

                            //Gradle 6.x emits warnings when repos are not HTTP and not set to allow non-secure protocol. Using reflection to maintain backwards compatibility
                            if (GradleVersion.current().baseVersion >= GradleVersion.version("6.0") && nebulaOssPublishingExtension.netflixOssRepositoryBaseUrl.get().startsWith("http://")) {
                                (this::class.java).getDeclaredMethod("setAllowInsecureProtocol", Boolean::class.java)
                                    .invoke(this, true)
                            }
                        }
                    }
                }
            }
        }
    }
}
