package nebula.plugin.publishing.pom

import spock.lang.Subject

@Subject(PomParser)
class PomParserSpec extends BasePomSpec {

    def 'should parse valid POM file'() {
        setup:
        File pomFile = findPomFile("/my-module.pom")

        when:
        def model = PomParser.parse(pomFile)

        then:
        model.groupId == 'com.netflix.nebula'
        model.artifactId == 'my-module'
        model.version == '1.0.0'
        model.description == 'my-module description'
        model.url == 'https://github.com/nebula-plugins/my-module'
        model.developers[0].name == 'Netflix Open Source Development'
        model.developers[0].email == 'netflixoss@netflix.com'
        model.scm.url == 'https://github.com/nebula-plugins/my-module.git'
        model.licenses[0].name == 'The Apache Software License, Version 2.0'
        model.licenses[0].url == 'http://www.apache.org/licenses/LICENSE-2.0.txt'
    }

    def 'should fail if POM file is invalid'() {
        setup:
        File pomFile = findPomFile("/invalid.pom")

        when:
         PomParser.parse(pomFile)

        then:
        def ex = thrown(MavenCentralPomVerificationException)
        ex.message.contains 'Error while trying to read nebula publication Pom file'
    }

    def 'should fail if POM file does not exist'() {
        when:
        PomParser.parse(new File("/this-does-not-exist.pom"))

        then:
        def ex = thrown(MavenCentralPomVerificationException)
        ex.message.contains 'Error while trying to read nebula publication Pom file'
    }
}
