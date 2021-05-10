package nebula.plugin.publishing

import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory

class EnvironmentReader(private val providerFactory: ProviderFactory) {
    fun findPropertyValue(project: Project,
                          envVariableName: String,
                          namespacedPropertyName: String,
                          propertyName: String
    ) : String? {
        val propertyValueFromEnv = readEnvVariable(envVariableName)
        return when {
            propertyValueFromEnv != null -> {
                propertyValueFromEnv
            }
            project.hasProperty(propertyName) -> {
                project.prop(propertyName)
            }
            project.hasProperty(namespacedPropertyName) -> {
                project.prop(namespacedPropertyName)
            }
            else -> null
        }
    }

    private fun readEnvVariable(envVariableName: String) : String? {
        val envVariable = providerFactory.environmentVariable(envVariableName).forUseAtConfigurationTime()
        return if(envVariable.isPresent) envVariable.get() else null
    }

    private fun Project.prop(s: String): String? = project.findProperty(s) as String?
}
