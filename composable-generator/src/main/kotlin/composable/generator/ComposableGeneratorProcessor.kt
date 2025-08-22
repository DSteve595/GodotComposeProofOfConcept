@file:OptIn(KspExperimental::class)

package composable.generator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName

class ComposableGeneratorProcessor(
  private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

  private var round = 0

  private lateinit var baseControlType: KSType
  private lateinit var signalType: KSType

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (round > 0) return emptyList()
    baseControlType = resolver.getClassDeclarationByName("godot.api.Control")!!.asType(emptyList())
    signalType = resolver.getClassDeclarationByName("godot.core.Signal")!!.asStarProjectedType()
    resolver.getNewFiles()
    val controlDeclarations = sequence {
      val godotControlTypes = resolver.getDeclarationsFromPackage("godot.api")
        .filterIsInstance<KSClassDeclaration>()
        .filter { baseControlType.isAssignableFrom(it.asStarProjectedType()) }
      yieldAll(godotControlTypes)

      val annotatedControlTypes = resolver.getSymbolsWithAnnotation("godot.annotation.RegisterClass")
        .filterIsInstance<KSClassDeclaration>()
        .filter { baseControlType.isAssignableFrom(it.asStarProjectedType()) }
        .filter { it.getDeclaredProperties().any { property -> property.isPublic() && !property.isSignal() } }
      yieldAll(annotatedControlTypes)
    }
    controlDeclarations.forEach { controlDeclaration ->
      val packageName = controlDeclaration.packageName.asString()
      val fileName = "${controlDeclaration.simpleName.asString()}_generatedComposable"
      environment.codeGenerator.createNewFile(
        dependencies = Dependencies(
          aggregating = false,
          sources = listOfNotNull(controlDeclaration.containingFile).toTypedArray()
        ),
        packageName = packageName,
        fileName = fileName,
        extensionName = "kt",
      ).use { output ->
        output.bufferedWriter().use { writer ->
          fileSpecForControl(packageName, fileName, controlDeclaration).writeTo(writer)
        }
      }
    }
    round++
    return emptyList()
  }

  private fun fileSpecForControl(
    packageName: String,
    fileName: String,
    controlDeclaration: KSClassDeclaration,
  ): FileSpec = FileSpec.builder(packageName = packageName, fileName = fileName).run {
    val isBaseControl = controlDeclaration.qualifiedName!!.asString() == "godot.api.Control"
    fun KSType.implementAsNullable(): Boolean = when (declaration.qualifiedName!!.asString()) {
      "kotlin.Boolean" -> false
      "kotlin.Double" -> false
      "kotlin.Float" -> false
      "kotlin.Int" -> false
      else -> true
    }

    fun defaultInitializerFor(type: KSType): String = when {
      type.implementAsNullable() -> "null"
      type.declaration.qualifiedName!!.asString() == "kotlin.Boolean" -> "false"
      type.declaration.qualifiedName!!.asString() == "kotlin.Double" -> "0.0"
      type.declaration.qualifiedName!!.asString() == "kotlin.Float" -> "0f"
      type.declaration.qualifiedName!!.asString() == "kotlin.Int" -> "0"
      else -> "null"
    }

    val superControlTypes = controlDeclaration.superTypes.map { it.resolve() }
      .filter { baseControlType.isAssignableFrom(it) }
    // TODO missing other Variant setters, and stuff like setAnchorsPreset
    val allProperties = controlDeclaration.getAllProperties()
      .filter { it.isMutable || it.type.resolve().declaration.qualifiedName!!.asString() == "godot.core.Vector2" }
      .filter { it.isPublic() }
      .filter { !it.isSignal() }
    val declaredProperties = controlDeclaration.getDeclaredProperties()
      .filter { it.isMutable || it.type.resolve().declaration.qualifiedName!!.asString() == "godot.core.Vector2" }
      .filter { it.isPublic() }
      .filter { !it.isSignal() }

    this
      .addType(
        TypeSpec.interfaceBuilder("${controlDeclaration.simpleName.asString()}Props")
          .apply {
            superControlTypes.forEach { superControlType ->
              addSuperinterface(
                ClassName(
                  superControlType.declaration.packageName.asString(),
                  "${superControlType.declaration.simpleName.asString()}Props"
                )
              )
            }
            (if (isBaseControl) allProperties else declaredProperties).forEach { property ->
              addProperty(
                PropertySpec.builder(
                  property.simpleName.asString(),
                  property.type.resolve().run { toTypeName().copy(nullable = implementAsNullable()) }
                )
                  .mutable()
                  .build()
              )
            }
          }
          .build()
      )
      .apply {
        if (isBaseControl) {
          addType(
            TypeSpec.interfaceBuilder("${controlDeclaration.simpleName.asString()}PropsImpl")
              .addModifiers(KModifier.INTERNAL)
              .addSuperinterface(ClassName("godot.api", "ControlProps"))
              .addTypeVariable(TypeVariableName("T", ClassName(packageName, controlDeclaration.qualifiedName!!.asString())))
              .apply {
                allProperties.forEach { property ->
                  val type = property.type.resolve()
                  addProperty(
                    PropertySpec.builder(
                      property.simpleName.asString(),
                      type.toTypeName().copy(nullable = type.implementAsNullable())
                    )
                      .addModifiers(KModifier.OVERRIDE)
                      .mutable()
                      .build()
                  )
                }
              }
              .addFunction(
                FunSpec.builder("updateNodeProperties")
                  .receiver(
                    ClassName("androidx.compose.runtime", "Updater")
                      .parameterizedBy(TypeVariableName("T"))
                  )
                  .build()
              )
              .addFunction(FunSpec.builder("clear").build())
              .build()
          )
        } else {
          addType(
            TypeSpec.classBuilder("${controlDeclaration.simpleName.asString()}PropsImpl")
              .addModifiers(KModifier.INTERNAL)
              .addSuperinterface(ClassName(packageName, "${controlDeclaration.simpleName.asString()}Props"))
              .addSuperinterface(
                ClassName("godot.api", "ControlPropsImpl")
                  .parameterizedBy(ClassName(packageName, controlDeclaration.qualifiedName!!.asString()))
              )
              .apply {
                allProperties.forEach { property ->
                  val type = property.type.resolve()
                  val initializedValue = defaultInitializerFor(type)
                  addProperty(
                    PropertySpec.builder(
                      property.simpleName.asString() + "Set",
                      ClassName("kotlin", "Boolean")
                    )
                      .mutable()
                      .initializer("false")
                      .build()
                  )
                  run {
                    val storedTypeName = type.toTypeName().copy(nullable = type.implementAsNullable())
                    addProperty(
                      PropertySpec.builder(
                        property.simpleName.asString(),
                        storedTypeName
                      )
                        .addModifiers(KModifier.OVERRIDE)
                        .mutable()
                        .initializer("%L", initializedValue)
                        .setter(
                          FunSpec.setterBuilder()
                            .addParameter("value", storedTypeName)
                            .addStatement("field = value")
                            .addStatement("%L = true", property.simpleName.asString() + "Set")
                            .build()
                        )
                        .build()
                    )
                  }
                }
              }
              .addFunction(
                FunSpec.builder("updateNodeProperties")
                  .receiver(
                    ClassName("androidx.compose.runtime", "Updater")
                      .parameterizedBy(ClassName(packageName, controlDeclaration.qualifiedName!!.asString()))
                  )
                  .addModifiers(KModifier.OVERRIDE)
                  .apply {
                    allProperties.forEach { property ->
                      val type = property.type.resolve()
                      val rhs = if (type.implementAsNullable()) "it!!" else "it"
                      val setter = when {
                        type.declaration.qualifiedName!!.asString() == "godot.core.Vector2" ->
                          "this.set${property.simpleName.asString().replaceFirstChar { it.uppercase() }}($rhs)"
                        else -> "this.${property.simpleName.asString()} = $rhs"
                      }
                      addStatement(
                        "set(%L) { if (this@%L.%L) $setter }",
                        property.simpleName.asString(),
                        controlDeclaration.simpleName.asString() + "PropsImpl",
                        property.simpleName.asString() + "Set",
                      )
                    }
                  }
                  .build()
              )
              .addFunction(
                FunSpec.builder("clear")
                  .addModifiers(KModifier.OVERRIDE)
                  .apply {
                    allProperties.forEach { property ->
                      val type = property.type.resolve()
                      val initializedValue = defaultInitializerFor(type)
                      // Reset property to its initial default, then mark it as not set
                      addStatement("%L = %L", property.simpleName.asString(), initializedValue)
                      addStatement("%L = false", property.simpleName.asString() + "Set")
                    }
                  }
                  .build()
              )
              .build()
          )
        }
      }
      .apply {
        if (!isBaseControl && controlDeclaration.primaryConstructor?.isPublic() == true) {
          addFunction(
            FunSpec.builder(controlDeclaration.simpleName.asString())
              .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
              .addParameter(
                ParameterSpec.builder(
                  "props",
                  LambdaTypeName.get(
                    receiver = ClassName(packageName, "${controlDeclaration.simpleName.asString()}Props"),
                    returnType = UNIT
                  )
                )
                  .defaultValue("{ }")
                  .build()
              )
              .addParameter(
                ParameterSpec.builder(
                  "content",
                  LambdaTypeName.get(returnType = UNIT).copy(
                    annotations = listOf(
                      AnnotationSpec.builder(
                        ClassName("androidx.compose.runtime", "Composable")
                      ).build()
                    )
                  )
                )
                  .build()
              )
              .addCode(
                """
              godot.ControlNode(
                factory = { %L() },
                propsImpl = androidx.compose.runtime.remember { %L() },
                propsBlock = props,
                content = content,
              )
            """.trimIndent(),
                ClassName(packageName, controlDeclaration.simpleName.asString()),
                ClassName(packageName, "${controlDeclaration.simpleName.asString()}PropsImpl"),
              )
              .build()
          )
        }
      }
      .build()
  }

  private fun KSPropertyDeclaration.isSignal(): Boolean = signalType.isAssignableFrom(type.resolve())
}
