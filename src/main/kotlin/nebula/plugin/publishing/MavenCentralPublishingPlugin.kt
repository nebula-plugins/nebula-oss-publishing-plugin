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

import de.marcphilipp.gradle.nexus.InitializeNexusStagingRepository
import de.marcphilipp.gradle.nexus.NexusPublishExtension
import de.marcphilipp.gradle.nexus.NexusRepository
import io.codearte.gradle.nexus.NexusStagingExtension
import io.codearte.gradle.nexus.NexusStagingPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Configures publication to staging repo in sonatype and makes sure staging repositories are closed/released
 */
class MavenCentralPublishingPlugin : Plugin<Project> {
    companion object {
        private val serverUrlToStagingRepoUrl = ConcurrentHashMap<URI, URI>()
        const val closeAndPromoteRepositoryTaskName = "closeAndReleaseRepository"
        const val sonatypeOssRepositoryUrl = "https://oss.sonatype.org/service/local/"
        const val nexusStagingPluginId = "io.codearte.nexus-staging"
        const val nebulaMavenPublishPluginId = "nebula.maven-publish"
    }

    override fun apply(project: Project) {
        if (!isFinalRelease(project)) {
            return
        }

        if (project.rootProject == project) {
            project.pluginManager.apply(NexusStagingPlugin::class)
        }

        val nebulaOssPublishingExtension = project.rootProject.extensions.findByType(NebulaOssPublishingExtension::class.java) ?: project.rootProject.extensions.create("nebulaOssPublishing", NebulaOssPublishingExtension::class.java)

        val nexusPublishExtension = project.extensions.create<NexusPublishExtension>("nexusPublishExtension", project)
        nexusPublishExtension.packageGroup.set(nebulaOssPublishingExtension.packageGroup.get())
        nexusPublishExtension.repositories.sonatype {
            nexusUrl.set(URI(sonatypeOssRepositoryUrl))
            username.set(nebulaOssPublishingExtension.sonatypeUsername.get())
            password.set(nebulaOssPublishingExtension.sonatypePassword.get())
            stagingProfileId.set(nebulaOssPublishingExtension.stagingProfileId.orNull)
        }

        nexusPublishExtension.repositories.all {
            project.tasks.register("publishTo${name.capitalize()}") {
                description = "Publishes all Maven publications produced by this project to the '${this@all.name}' Nexus repository."
                group = PublishingPlugin.PUBLISH_TASK_GROUP
            }
            project.tasks
                .register<InitializeNexusStagingRepository>("initialize${name.capitalize()}StagingRepository", project.objects, nexusPublishExtension, this,
                    serverUrlToStagingRepoUrl
                )
        }
        nexusPublishExtension.repositories.whenObjectRemoved {
            project.tasks.remove(project.tasks.named("publishTo${name.capitalize()}") as Any)
            project.tasks.remove(project.tasks.named("initialize${name.capitalize()}StagingRepository") as Any)
        }

        project.afterEvaluate {
            val nexusRepositories = addMavenRepositories(project, nexusPublishExtension)
            nexusRepositories.forEach { (nexusRepo, mavenRepo) ->
                val publishToNexusTask = project.tasks.named("publishTo${nexusRepo.name.capitalize()}")
                val initializeTask = project.tasks.withType(InitializeNexusStagingRepository::class)
                    .named("initialize${nexusRepo.name.capitalize()}StagingRepository")
                configureTaskDependencies(project, publishToNexusTask, initializeTask, mavenRepo)
            }
        }

        project.rootProject.plugins.withId(nexusStagingPluginId) {
            val nexusStagingExtension = project.rootProject.the<NexusStagingExtension>()
            nexusStagingExtension.username = nebulaOssPublishingExtension.sonatypeUsername.get()
            nexusStagingExtension.password = nebulaOssPublishingExtension.sonatypePassword.get()
            nexusStagingExtension.packageGroup = nebulaOssPublishingExtension.packageGroup.get()
            nexusStagingExtension.stagingProfileId = nebulaOssPublishingExtension.stagingProfileId.orNull
        }

        project.afterEvaluate {
            project.plugins.withId(nebulaMavenPublishPluginId) {
                project.rootProject.tasks.named("postRelease").configure {
                    this.dependsOn(project.tasks.withType(PublishToMavenRepository::class.java))
                }
            }

            project.rootProject.plugins.withId(nexusStagingPluginId) {
                project.rootProject.tasks.named("postRelease").configure {
                    this.dependsOn(project.rootProject.tasks.named(closeAndPromoteRepositoryTaskName))
                    this.mustRunAfter(project.tasks.withType(PublishToMavenRepository::class.java))
                }
            }
        }
    }


    private fun addMavenRepositories(project: Project, extension: NexusPublishExtension): Map<NexusRepository, MavenArtifactRepository> {
        return extension.repositories.associateWith { nexusRepo ->
            project.the<PublishingExtension>().repositories.maven {
                name = nexusRepo.name
                url = nexusRepo.nexusUrl.get()
                credentials {
                    username = nexusRepo.username.orNull
                    password = nexusRepo.password.orNull
                }
            }
        }
    }

    private fun configureTaskDependencies(project: Project, publishToNexusTask: TaskProvider<Task>, initializeTask: TaskProvider<InitializeNexusStagingRepository>, nexusRepository: MavenArtifactRepository) {
        val publishTasks = project.tasks
            .withType<PublishToMavenRepository>()
            .matching { it.repository == nexusRepository }
        publishToNexusTask.configure { dependsOn(publishTasks) }
        // PublishToMavenRepository tasks may not yet have been initialized
        project.afterEvaluate {
            publishTasks.configureEach {
                dependsOn(initializeTask)
                doFirst { logger.info("Uploading to {}", repository.url) }
            }
        }
    }

    private fun isFinalRelease(project: Project): Boolean {
        return project.gradle.startParameter.taskNames.contains("final") || project.gradle.startParameter.taskNames.contains(":final")
    }
}