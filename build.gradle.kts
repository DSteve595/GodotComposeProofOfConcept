plugins {
  id("com.utopia-rise.godot-kotlin-jvm") version "0.13.1-4.4.1"
  id("org.jetbrains.compose") version "1.8.2"
  id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" // kotlin version
}

repositories {
  mavenCentral()
  google()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
  jvmToolchain(17)
}

godot {
  registrationFileBaseDir.set(projectDir.resolve("gdj"))

  isRegistrationFileGenerationEnabled.set(true)

  isGodotCoroutinesEnabled.set(true)
}

dependencies {
  implementation(compose.runtime)
  compileOnly(compose.ui)
}
