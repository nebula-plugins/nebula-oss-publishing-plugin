package nebula.plugin.publishing.pom

import spock.lang.Specification

class BasePomSpec extends Specification {
    File findPomFile(String pomPath) {
        return new File(this.class.getResource(pomPath).toURI())
    }
}
