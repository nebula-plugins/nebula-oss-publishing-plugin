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

import nebula.test.IntegrationSpec
import org.gradle.api.plugins.JavaPlugin

class NebulaOssPublishingPluginSpec extends IntegrationSpec {

    def 'Apply plugin without failures'() {
        buildFile << """
            plugins {
              id "nebula.maven-publish" version "17.3.2"
            }

            group = 'test'
            ${applyPlugin(JavaPlugin)}
            ${applyPlugin(NebulaOssPublishingPlugin)}
            
            nebulaOssPublishing {
                netflixOssRepositoryBaseUrl = "http://"
                netflixOssRepository = "my-releases"
                netflixOssUsername = "user"
                netflixOssPassword = "password"               
            }
        """.stripIndent()

        expect:
        runTasksSuccessfully('build')
    }
}
