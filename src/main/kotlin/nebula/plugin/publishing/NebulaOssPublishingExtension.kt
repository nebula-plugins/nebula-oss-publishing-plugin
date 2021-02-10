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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class NebulaOssPublishingExtension(objects: ObjectFactory) {
    val packageGroup : Property<String> = objects.property()
    val stagingProfileId : Property<String> = objects.property()
    val netflixOssRepositoryBaseUrl : Property<String> = objects.property()
    val netflixOssRepository : Property<String> = objects.property()
    val netflixOssUsername : Property<String> = objects.property()
    val netflixOssPassword : Property<String> = objects.property()
    val sonatypeUsername: Property<String> = objects.property()
    val sonatypePassword: Property<String> = objects.property()
    val signingKey:  Property<String> = objects.property()
    val signingPassword:  Property<String> = objects.property()
}
