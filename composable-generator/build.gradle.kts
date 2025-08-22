plugins {
  kotlin("jvm")
}

repositories {
  mavenCentral()
  google()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  compileOnly("com.google.devtools.ksp:symbol-processing-api:2.1.10-1.0.31")
  implementation("com.squareup:kotlinpoet:2.2.0")
  implementation("com.squareup:kotlinpoet-ksp:2.2.0")
}
