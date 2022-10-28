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
    id("com.netflix.nebula.plugin-plugin") version "19.0.1"
    kotlin("jvm") version "1.7.10"
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
    implementation("io.github.gradle-nexus:publish-plugin:1.0.0")
    implementation("org.apache.maven:maven-model:3.6.2")
    constraints {
        val kotlinVersion by extra("1.6.21")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    }
}


pluginBundle {
    website = "https://github.com/nebula-plugins/nebula-oss-publishing-plugin"
    vcsUrl = "https://github.com/nebula-plugins/nebula-oss-publishing-plugin.git"
    description = "Plugins to configure common configuration"
    description = "Uploads candidate and final artifacts to bintray with Nebula defaults"
    tags = listOf("nebula", "nexus", "sonatype")
}

gradlePlugin {
    plugins {
        create("nebulaOssPublishing") {
            id = "com.netflix.nebula.oss-publishing"
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

javaCrossCompile {
    disableKotlinSupport = true
}