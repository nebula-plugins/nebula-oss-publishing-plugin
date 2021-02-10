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

import nebula.test.IntegrationSpec
import org.gradle.api.plugins.JavaPlugin

class NebulaSigningPluginSpec extends IntegrationSpec {

    def 'register signing task'() {
        buildFile << """
            $commonBuildFile
           
            nebulaOssPublishing {
                signingKey = 'something'
                signingPassword = 'something'
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully('publishNebulaPublicationToMavenLocal', '--dry-run')

        then:
        result.standardOutput.contains(':signNebulaPublication')
    }

    def 'do not register signing task if key and password are not present'() {
        buildFile << commonBuildFile.stripIndent()

        when:
        def result = runTasksSuccessfully('publishNebulaPublicationToMavenLocal', '--dry-run')

        then:
        !result.standardOutput.contains(':signNebulaPublication')
    }

    def 'only signs nebula publication'() {
        buildFile << """
            $commonBuildFile

            nebulaOssPublishing {
                signingKey = 'something'
                signingPassword = 'something'
            }

            publishing {
                publications {
                    customMaven(MavenPublication) {
                        groupId = 'org.gradle.sample'
                        artifactId = 'library'
                        version = '1.1'
            
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully('publishCustomMavenPublicationToMavenLocal', 'publishNebulaPublicationToMavenLocal', '--dry-run')

        then:
        result.standardOutput.contains(':signNebulaPublication')
        !result.standardOutput.contains(':signCustomMavenPublicationn')
    }

    private final String getCommonBuildFile() {
        return """
            plugins {
              id "nebula.maven-publish" version "17.3.2"
            }

            group = 'test'
            ${applyPlugin(JavaPlugin)}
            ${applyPlugin(NebulaSigningPlugin)}
        """
    }
}
