plugins {
  kotlin("multiplatform") version KOTLIN_VERSION apply false
  kotlin("plugin.serialization") version KOTLIN_VERSION apply false
//  kotlin("multiplatform") version KOTLIN_VERSION apply false
//  id("kotlin-dce-js") version KOTLIN_VERSION apply false
//  id("kotlinx-serialization") version KOTLIN_VERSION apply false
}

buildscript {
  repositories {
    google()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.4.2")//todo const
  }
}

allprojects {//todo allprojects bad?
//  buildDir = File("/dev/shm/$name")
//  version = "1.0"
  repositories {
    mavenLocal {
      url = uri("${rootProject.projectDir}/save_dependencies")
      // com/jetbrains/intellij/java/java-compiler-ant-tasks/211.7628.21.2111.7579519/java-compiler-ant-tasks-211.7628.21.2111.7579519.pom
    }
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
    //maven { setUrl("https://dl.bintray.com/kotlin/exposed") }
    maven { setUrl("https://kotlin.bintray.com/ktor") }
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()

    if (USE_KOTLIN_DEV_REPOSITORY) {
      maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
      maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
      maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
      maven { setUrl("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
    }
  }
  tasks.withType(AbstractTestTask::class) {
    testLogging {
      showStandardStreams = true
      events("passed", "failed")
    }
  }
  //todo check difference if use afterEvaluate { tasks... }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = MIN_JDK_VERSION.kotlinTarget
    if (COMPOSE_WORKAROUND) {
      kotlinOptions {
        freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true")
      }
    }
  }
  if (ConfBuild.TRACE_GRADLE_TASKS) {
    afterEvaluate {
      tasks.all {
        doFirst {
          println("before task ${this.path}")
        }
        doLast {
          println("after task ${this.path}")
        }
      }
    }
  }
}
