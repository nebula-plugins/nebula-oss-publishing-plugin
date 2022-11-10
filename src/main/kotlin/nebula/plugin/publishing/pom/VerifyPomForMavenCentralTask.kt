package nebula.plugin.publishing.pom

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault
open class VerifyPomForMavenCentralTask @Inject constructor(objects: ObjectFactory): DefaultTask() {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    val pomFile: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun verifyPom() {
        val model = PomParser.parse(pomFile.asFile.get())
        MavenCentralPomVerifier.verify(model)
    }
}
