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
    id("nebula.plugin-plugin") version "15.0.1"
    kotlin("jvm") version "1.4.30"
}

description = "Nebula Netflix OSS Publishing plugin"

group = "com.netflix.nebula"

val contact = Contact("roberto@perezalcolea.info")
contact.moniker = "Roberto Perez"
contact.github = "rpalcolea"
contacts {
    people.set("roberto@perezalcolea.info", contact)
}

dependencies {
    implementation("de.marcphilipp.gradle:nexus-publish-plugin:0.4.0")
    implementation("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.22.0")
    testImplementation("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.22.0")
    constraints {
        val kotlinVersion by extra("1.4.30")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    }
}


pluginBundle {
    website = "https://github.com/nebula-plugins/nebula-oss-publishing-plugin"
    vcsUrl = "https://github.com/nebula-plugins/nebula-oss-publishing-plugin.git"
    description = "Plugins to configure common configuration"
    description = "Uploads candidate and final artifacts to bintray with Nebula defaults"
    tags = listOf("nebula", "bintray")

    plugins {
        create("nebulaOssPublishing") {
            id = "nebula.oss-publishing"
            displayName = "Nebula OSS Publishing plugin"
            description = "Publishes nebula projects to Netflix OSS repositories and Maven Central"
            tags = listOf("nebula", "nexus", "sonatype")
        }
    }
}

gradlePlugin {
    plugins {
        create("nebulaOssPublishing") {
            id = "nebula.oss-publishing"
            displayName = "Nebula OSS Publishing plugin"
            description = "Publishes nebula projects to Netflix OSS repositories and Maven Central"
            implementationClass = "nebula.plugin.publishing.NebulaOssPublishingPlugin"
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}
