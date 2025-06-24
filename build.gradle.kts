
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
    id("com.netflix.nebula.plugin-plugin") version "22.0.2"
    kotlin("jvm") version "2.2.0"
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
    constraints {
        val kotlinVersion by extra("2.0.20")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    }
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
    }
}


javaCrossCompile {
    disableKotlinSupport = true
}
