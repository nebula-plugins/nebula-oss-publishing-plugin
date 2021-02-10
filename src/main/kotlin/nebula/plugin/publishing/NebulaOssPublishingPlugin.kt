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
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import javax.inject.Inject

/**
 * Configures artifact signing and publication to NetflixOSS and Maven Central
 */
open class NebulaOssPublishingPlugin @Inject constructor(private val providerFactory: ProviderFactory): Plugin<Project> {
    companion object {
        const val netflixOssDefaultRepositoryBaseUrl = "https://netflixoss.jfrog.io/artifactory"
        const val netflixOssGradlePluginsRepository = "gradle-plugins"
        const val netflixOssSnapshotsRepository = "maven-oss-snapshots"
        const val netflixOssCandidatesRepository = "maven-oss-candidates"
        const val netflixOssReleasesRepository = "maven-oss-releases"
    }

    override fun apply(project: Project) {
        val nebulaOssPublishingExtension = project.rootProject.extensions.findByType(NebulaOssPublishingExtension::class.java) ?: project.rootProject.extensions.create("nebulaOssPublishing", NebulaOssPublishingExtension::class.java)
        setExtensionDefaults(nebulaOssPublishingExtension, project)

        project.pluginManager.apply(NebulaSigningPlugin::class)
        project.pluginManager.apply(MavenCentralPublishingPlugin::class)
        project.pluginManager.apply(NebulaOssRepositoriesPlugin::class)

    }

    private fun setExtensionDefaults(nebulaOssPublishingExtension: NebulaOssPublishingExtension, project: Project) {
        setMavenCentralCredentials(nebulaOssPublishingExtension, project)
        setStagingProfileId(nebulaOssPublishingExtension, project)
        setPackageGroup(nebulaOssPublishingExtension, project)
        setSigningKey(nebulaOssPublishingExtension, project)
        setSigningPassword(nebulaOssPublishingExtension, project)
        setNetflixOssCredentials(nebulaOssPublishingExtension, project)
        setNetflixOssRepositoryBaseUrl(nebulaOssPublishingExtension, project)
        setDefaultNetflixOssRepository(nebulaOssPublishingExtension, project)
    }

    private fun setStagingProfileId(extension: NebulaOssPublishingExtension, project: Project) {
        val stagingProfileId = findPropertyValue(
            project,
            "NETFLIX_OSS_SONATYPE_STAGING_PROFILE_ID",
            "sonatype.stagingProfileId",
            "sonatypeStagingProfileId"
        )
        if(!stagingProfileId.isNullOrBlank()) {
            extension.stagingProfileId.convention(stagingProfileId)
        }
    }

    private fun setPackageGroup(extension: NebulaOssPublishingExtension, project: Project) {
        val packageGroup = findPropertyValue(
            project,
            "NETFLIX_OSS_SONATYPE_PACKAGE_GROUP",
            "sonatype.packageGroup",
            "sonatypePackageGroup"
        ) ?: project.group.toString().split(".").take(2).joinToString(".")

        if(packageGroup.isNotBlank()) {
            extension.packageGroup.convention(packageGroup)
        }
    }

    private fun setSigningKey(extension: NebulaOssPublishingExtension, project: Project) {
        val signingKey = findPropertyValue(
            project,
            "NETFLIX_OSS_SIGNING_KEY",
            "sonatype.signingKey",
            "netflixOssSigningKey"
        )
        if(!signingKey.isNullOrBlank()) {
            extension.signingKey.convention(signingKey)
        }
    }

    private fun setSigningPassword(extension: NebulaOssPublishingExtension, project: Project) {
        val signingPassword = findPropertyValue(project,
            "NETFLIX_OSS_SIGNING_PASSWORD",
            "sonatype.signingPassword",
            "netflixOssSigningPassword")
        if(!signingPassword.isNullOrBlank()) {
            extension.signingPassword.convention(signingPassword)
        }
    }

    private fun setNetflixOssCredentials(extension: NebulaOssPublishingExtension, project: Project) {
        val netflixOssUsername = findPropertyValue(project,
            "NETFLIX_OSS_REPO_USERNAME",
            "netflixOss.username",
            "netflixOssUsername")
        if(!netflixOssUsername.isNullOrBlank()) {
            extension.netflixOssUsername.convention(netflixOssUsername)
        }

        val netflixOssPassword = findPropertyValue(project,
            "NETFLIX_OSS_REPO_PASSWORD",
            "netflixOss.password",
            "netflixOssPassword")
        if(!netflixOssPassword.isNullOrBlank()) {
            extension.netflixOssPassword.convention(netflixOssPassword)
        }
    }

    private fun setMavenCentralCredentials(extension: NebulaOssPublishingExtension, project: Project) {
        val sonatypeUsername = findPropertyValue(project,
            "NETFLIX_OSS_SONATYPE_USERNAME",
            "sonatype.username",
            "sonatypeUsername")
        if(!sonatypeUsername.isNullOrBlank()) {
            extension.sonatypeUsername.convention(sonatypeUsername)
        }

        val sonatypePassword = findPropertyValue(project,
            "NETFLIX_OSS_SONATYPE_PASSWORD",
            "sonatype.password",
            "sonatypePassword")
        if(!sonatypePassword.isNullOrBlank()) {
            extension.sonatypePassword.convention(sonatypePassword)
        }
    }

    private fun setNetflixOssRepositoryBaseUrl(extension: NebulaOssPublishingExtension, project: Project) {
        val repositoryBaseUrl = findPropertyValue(
            project,
            "NETFLIX_OSS_REPOSITORY_BASE_URL",
            "netflixOss.repositoryBaseUrl",
            "netflixOssRepositoryBaseUrl"
        )
        if(!repositoryBaseUrl.isNullOrBlank()) {
            extension.netflixOssRepositoryBaseUrl.convention(repositoryBaseUrl)
        } else {
            extension.netflixOssRepositoryBaseUrl.convention(netflixOssDefaultRepositoryBaseUrl)
        }
    }

    private fun setDefaultNetflixOssRepository(extension: NebulaOssPublishingExtension, project: Project) {
        project.rootProject.pluginManager.withPlugin("nebula.plugin-plugin") {
            extension.netflixOssRepository.convention(netflixOssGradlePluginsRepository)
        }
        project.rootProject.pluginManager.withPlugin("nebula.netflixoss") {
            when {
                projectExecutionHasTask(project, "snapshot") || projectExecutionHasTask(project, "devSnapshot") || projectExecutionHasTask(project, "immutableSnapshot") -> {
                    extension.netflixOssRepository.convention(netflixOssSnapshotsRepository)
                }
                projectExecutionHasTask(project, "candidate") -> {
                    extension.netflixOssRepository.convention(netflixOssCandidatesRepository)
                }
                projectExecutionHasTask(project, "final") -> {
                    extension.netflixOssRepository.convention(netflixOssReleasesRepository)
                }
            }
        }
    }

    private fun projectExecutionHasTask(project: Project, task: String): Boolean {
        return project.gradle.startParameter.taskNames.contains(task) || project.gradle.startParameter.taskNames.contains(":${task}")
    }

    private fun findPropertyValue(project: Project,
                                  envVariableName: String,
                                  namespacedPropertyName: String,
                                  propertyName: String
    ) : String? {
        val propertyValueFromEnv = readEnvVariable(envVariableName)
        return when {
            propertyValueFromEnv != null -> {
                propertyValueFromEnv
            }
            project.hasProperty(propertyName) -> {
                project.prop(propertyName)
            }
            project.hasProperty(namespacedPropertyName) -> {
                project.prop(namespacedPropertyName)
            }
            else -> null
        }
    }

    private fun readEnvVariable(envVariableName: String) : String? {
        val envVariable = providerFactory.environmentVariable(envVariableName).forUseAtConfigurationTime()
        return if(envVariable.isPresent) envVariable.get() else null
    }

    private fun Project.prop(s: String): String? = project.findProperty(s) as String?
}
