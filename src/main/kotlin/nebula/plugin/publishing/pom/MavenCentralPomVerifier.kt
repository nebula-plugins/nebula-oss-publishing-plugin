package nebula.plugin.publishing.pom

import org.apache.maven.model.Model
import org.gradle.api.GradleException

/**
 * Verifies if POM follows rules for Maven Central
 * @see <a href="http://maven.apache.org/repository/guide-central-repository-upload.html">http://maven.apache.org/repository/guide-central-repository-upload.html</a>
 */
class MavenCentralPomVerifier {
    companion object {

        @JvmStatic
        fun verify(model: Model) {
            val errors = mutableListOf<String>()
            if(model.groupId.isNullOrEmpty()) {
                errors.add("<groupId> must not be null or blank")
            }
            if(model.artifactId.isNullOrEmpty()) {
                errors.add("<artifactId> must not be null or blank")
            }
            if(model.version.isNullOrEmpty()) {
                errors.add("<version> must not be null or blank. Please configure a valid version in your project")
            }
            if(model.description.isNullOrEmpty()) {
                errors.add("<description> must not be null or blank. This information is added for you via nebula.netflixoss plugin")
            }
            if(model.url.isNullOrEmpty()) {
                errors.add("<url> must not be null or blank. This information is added for you via nebula.netflixoss plugin")
            }
            if(model.scm?.url.isNullOrEmpty()) {
                errors.add("<scm> url is required. This information is added for you via nebula.netflixoss plugin")
            }
            if(model.licenses.isEmpty()) {
                errors.add("<licenses> are required. This information is added for you via nebula.netflixoss plugin")
            }
            model.licenses.forEachIndexed { index, license ->
                if(license.name.isNullOrBlank()  || license.url.isNullOrBlank()) {
                    errors.add("License $index must have <name> and <url>. This information is added for you via nebula.netflixoss plugin")
                }
            }
            if(model.developers.isEmpty()) {
                errors.add("<developers> are required. Please add this information using gradle-contacts-plugin (https://github.com/nebula-plugins/gradle-contacts-plugin)")
            }
            model.developers.forEachIndexed { index, developer ->
                if(developer.name.isNullOrBlank() && developer.email.isNullOrBlank() && developer.id.isNullOrBlank()) {
                    errors.add("Developer $index must have one of: <name>, <email> or <id>. Please add this information using gradle-contacts-plugin (https://github.com/nebula-plugins/gradle-contacts-plugin)")
                }
            }
            model.dependencies.forEach { dependency ->
                if(dependency.version.contains(".+")) {
                    errors.add("Dependency ${dependency.groupId}:${dependency.artifactId}:${dependency.version} contains '.+'. This is an invalid dynamic version syntax.  Replace with a fixed version or standard mathematical notation e.g., [1.5,) for version 1.5 and higher. More info in https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN402")
                }
            }

            if (errors.isNotEmpty()) {
                throw MavenCentralPomVerificationException("POM verification for Maven Central failed.\n " +
                        "POM contains the following errors: \n" +
                        errors.joinToString(separator = "\n"))
            }
        }
    }
}
