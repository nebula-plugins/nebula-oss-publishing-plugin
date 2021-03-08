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

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import java.net.URI

/**
 * Configures publication to staging repo in sonatype and makes sure staging repositories are closed/released
 */
class MavenCentralPublishingPlugin : Plugin<Project> {
    companion object {
        const val closeAndPromoteRepositoryTaskName = "closeAndReleaseSonatypeStagingRepository"
        const val sonatypeOssRepositoryUrl = "https://oss.sonatype.org/service/local/"
        const val nebulaMavenPublishPluginId = "nebula.maven-publish"
    }

    override fun apply(project: Project) {
        if (project.rootProject != project) {
            return
        }

        if (!isFinalRelease(project) && !isCandidateReleaseAndPublishingToMavenCentralIsEnabled(project)) {
            return
        }

        project.pluginManager.apply(NexusPublishPlugin::class)

        val nebulaOssPublishingExtension = project.rootProject.extensions.findByType(NebulaOssPublishingExtension::class.java) ?: project.rootProject.extensions.create("nebulaOssPublishing", NebulaOssPublishingExtension::class.java)

        val nexusPublishExtension = project.extensions.findByType(NexusPublishExtension::class)
        if(nexusPublishExtension == null) {
            throw GradleException("Could not find registered NexusPublishExtension")
        }
        nexusPublishExtension.packageGroup.set(nebulaOssPublishingExtension.packageGroup.get())
        nexusPublishExtension.repositories.sonatype {
            nexusUrl.set(URI(sonatypeOssRepositoryUrl))
            username.set(nebulaOssPublishingExtension.sonatypeUsername.get())
            password.set(nebulaOssPublishingExtension.sonatypePassword.get())
            stagingProfileId.set(nebulaOssPublishingExtension.stagingProfileId.orNull)
        }

        project.afterEvaluate {
            project.plugins.withId(nebulaMavenPublishPluginId) {
                project.rootProject.tasks.named("postRelease").configure {
                    project.subprojects.forEach { subproject ->
                        this.dependsOn(subproject.tasks.withType(PublishToMavenRepository::class.java))
                    }
                }
            }

            project.rootProject.plugins.withType(NexusPublishPlugin::class) {
                project.rootProject.tasks.named("postRelease").configure {
                    this.dependsOn(project.rootProject.tasks.named(closeAndPromoteRepositoryTaskName))
                    project.subprojects.forEach { subproject ->
                        project.subprojects.forEach { subproject ->
                            this.mustRunAfter(subproject.tasks.withType(PublishToMavenRepository::class.java))
                        }
                    }
                }
            }
        }
    }

    private fun isFinalRelease(project: Project): Boolean {
        return project.gradle.startParameter.taskNames.contains("final") || project.gradle.startParameter.taskNames.contains(":final")
    }

    private fun isCandidateReleaseAndPublishingToMavenCentralIsEnabled(project: Project): Boolean {
        val isCandidate = project.gradle.startParameter.taskNames.contains("candidate") || project.gradle.startParameter.taskNames.contains(":candidate")
        val publishCandidateToMavenCentral = checkPublishCandidateToMavenCentral(project)
        return isCandidate && publishCandidateToMavenCentral
    }


    private fun checkPublishCandidateToMavenCentral(project: Project) : Boolean {
        if(project.hasProperty("netflixossAltCandidateRepo")) {
            val netflixossAltCandidateRepo = project.property("netflixossAltCandidateRepo").toString().toBoolean()
            if(!netflixossAltCandidateRepo) {
                return true
            }
        }

        if(project.hasProperty("netflixossPublishCandidatesToMavenCentral")) {
            return project.property("netflixossPublishCandidatesToMavenCentral").toString().toBoolean()
        }

        return false
    }

}
