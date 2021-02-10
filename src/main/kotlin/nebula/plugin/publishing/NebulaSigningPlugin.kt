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
import org.gradle.kotlin.dsl.apply
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

/**
 * Responsible for signing nebula publications
 */
class NebulaSigningPlugin : Plugin<Project> {
    companion object {
        const val nebulaPublicationName = "nebula"
    }

    override fun apply(project: Project) {
        val nebulaOssPublishingExtension = project.rootProject.extensions.findByType(NebulaOssPublishingExtension::class.java) ?: project.rootProject.extensions.create("nebulaOssPublishing", NebulaOssPublishingExtension::class.java)
        project.plugins.withId("nebula.maven-publish") {
            project.pluginManager.apply(SigningPlugin::class)
            project.afterEvaluate {
                // Do not configure signing task if key and password are not present
                if(!nebulaOssPublishingExtension.signingKey.isPresent || !nebulaOssPublishingExtension.signingPassword.isPresent) {
                    project.logger.info("signNebulaPublication was not configured: signingKey and/or signingPassword were not provided")
                    return@afterEvaluate
                }
                val signingExtension = project.extensions.getByType(SigningExtension::class.java)
                val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
                signingExtension.useInMemoryPgpKeys(nebulaOssPublishingExtension.signingKey.get(), nebulaOssPublishingExtension.signingPassword.get())
                // Only sign nebula publication
                signingExtension.sign(publishingExtension.publications.getByName(nebulaPublicationName))

                project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
                    this.dependsOn(project.tasks.withType(Sign::class.java))
                }
            }
        }
    }
}