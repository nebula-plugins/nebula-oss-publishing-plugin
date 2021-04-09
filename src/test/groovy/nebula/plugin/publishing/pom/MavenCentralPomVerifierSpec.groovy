package nebula.plugin.publishing.pom

class MavenCentralPomVerifierSpec  extends BasePomSpec {
    def 'POM file passes validation'() {
        setup:
        File pomFile = findPomFile("/my-module.pom")
        def pom = PomParser.parse(pomFile)

        when:
        MavenCentralPomVerifier.verify(pom)

        then:
        notThrown(MavenCentralPomVerificationException)
    }

    def 'collect errors from pom validation'() {
        setup:
        File pomFile = findPomFile("/my-module-with-invalid-metadata.pom")
        def pom = PomParser.parse(pomFile)

        when:
        MavenCentralPomVerifier.verify(pom)

        then:
        def ex = thrown(MavenCentralPomVerificationException)
        ex.message.contains('POM verification for Maven Central failed')
        ex.message.contains('<groupId> must not be null or blank')
        ex.message.contains('<artifactId> must not be null or blank')
        ex.message.contains('<version> must not be null or blank')
        ex.message.contains('<description> must not be null or blank')
        ex.message.contains('<url> must not be null or blank')
        ex.message.contains('<scm> url is required')
        ex.message.contains('<licenses> are required')
        ex.message.contains('<developers> are required')
    }

    def 'collect errors from pom validation - invalid license'() {
        setup:
        File pomFile = findPomFile("/my-module-with-invalid-license.pom")
        def pom = PomParser.parse(pomFile)

        when:
        MavenCentralPomVerifier.verify(pom)

        then:
        def ex = thrown(MavenCentralPomVerificationException)
        ex.message.contains('POM verification for Maven Central failed')
        ex.message.contains('License 0 must have <name> and <url>')
    }

    def 'collect errors from pom validation - invalid developer'() {
        setup:
        File pomFile = findPomFile("/my-module-with-invalid-developers.pom")
        def pom = PomParser.parse(pomFile)

        when:
        MavenCentralPomVerifier.verify(pom)

        then:
        def ex = thrown(MavenCentralPomVerificationException)
        ex.message.contains('POM verification for Maven Central failed')
        ex.message.contains('Developer 0 must have one of: <name>, <email> or <id>')
    }

    def 'collect errors from pom validation - invalid version (dynamic range)'() {
        setup:
        File pomFile = findPomFile("/my-module-with-invalid-dynamic-range.pom")
        def pom = PomParser.parse(pomFile)

        when:
        MavenCentralPomVerifier.verify(pom)

        then:
        def ex = thrown(MavenCentralPomVerificationException)
        ex.message.contains('POM verification for Maven Central failed')
        ex.message.contains('Dependency org.codehaus.groovy:groovy-all:2.5.+ contains \'.+\'. This is an invalid dynamic version syntax.  Replace with a fixed version or standard mathematical notation e.g., [1.5,) for version 1.5 and higher. More info in https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN402')
    }
}
