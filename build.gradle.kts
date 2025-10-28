
import nebula.plugin.contacts.Contact

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

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.netflix.nebula.plugin-plugin") version "23.+"
}

description = "Nebula Netflix OSS Publishing plugin"

group = "com.netflix.nebula"

val contact = Contact("nebula-plugins-oss@netflix.com")
contact.moniker = "Neubla Plugins Maintainers"
contact.github = "nebula-plugins"
contacts {
    people.set("rnebula-plugins-oss@netflix.com", contact)
}

dependencies {
    implementation("io.github.gradle-nexus:publish-plugin:2.0.0")
    implementation("org.apache.maven:maven-model:3.6.2")
    testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.14.0")
}

gradlePlugin {
    plugins {
        create("nebulaOssPublishing") {
            id = "com.netflix.nebula.oss-publishing"
            displayName = "Nebula OSS Publishing plugin"
            description = "Publishes nebula projects to Netflix OSS repositories and Maven Central"
            implementationClass = "nebula.plugin.publishing.NebulaOssPublishingPlugin"
            tags.set(listOf("nebula", "nexus", "sonatype"))
        }
        create("nebulaSigningPlugin") {
            id = "com.netflix.nebula.signing"
            displayName = "Nebula OSS Signing plugin"
            description = "Signs nebula projects"
            implementationClass = "nebula.plugin.publishing.NebulaSigningPlugin"
            tags.set(listOf("nebula"))
        }
    }
}

testing {
    suites {
        named<JvmTestSuite>("test"){
            useJUnitJupiter()
        }
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL // ALL helps when debugging gradle plugins
    gradleVersion = "9.1.0"
    distributionSha256Sum = "b84e04fa845fecba48551f425957641074fcc00a88a84d2aae5808743b35fc85"
}
