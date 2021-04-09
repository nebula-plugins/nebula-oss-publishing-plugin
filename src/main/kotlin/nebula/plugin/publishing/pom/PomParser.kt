package nebula.plugin.publishing.pom

import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import java.io.File
import java.io.FileReader

class PomParser {
    companion object {
        private val reader = MavenXpp3Reader()

        @JvmStatic
        fun parse(pomFile: File): Model {
            return try {
                reader.read(FileReader(pomFile))
            } catch (e: Exception) {
                throw MavenCentralPomVerificationException("Error while trying to read nebula publication Pom file", e)
            }
        }
    }
}
