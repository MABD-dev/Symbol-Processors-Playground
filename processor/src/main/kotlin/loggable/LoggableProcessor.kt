package loggable

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.*
import loggable.generators.LoggerImplClassGenerator

/**
 * @param tag if [tag] is blank, generated class name will be used as a tag
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Loggable(
    val tag: String = "",
)


/**
 * Mark function with this annotation to skip logging in that function
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.SOURCE)
annotation class NoLog

class LoggableProcessor(
    private val env: SymbolProcessorEnvironment
): SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val declarations = getLoggableAnnotationDeclarations(resolver)
        val privateInterfaces = declarations.filter { it.modifiers.contains(Modifier.PRIVATE) }

        privateInterfaces.forEach { declaration ->
            val interfaceName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
            env.logger.error("$interfaceName interface cannot be private")
        }

        declarations.forEach {
            val fileSpec = LoggerImplClassGenerator(it).generate()
            fileSpec.writeTo(env.codeGenerator, Dependencies(false))
        }
        return emptyList()
    }

    private fun getLoggableAnnotationDeclarations(resolver: Resolver): Sequence<KSClassDeclaration> {
        return resolver.getSymbolsWithAnnotation(Loggable::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
            .distinct()
            .filter { it.validate() }
            .filter { it.classKind == ClassKind.INTERFACE }
    }

}
