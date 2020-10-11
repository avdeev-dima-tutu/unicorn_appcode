// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package bootRuntime2.bundles

import bootRuntime2.main.BinTrayUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.SystemInfo
import java.io.File

abstract class Runtime(initialLocation:File) {

  val JAVA_FILE_NAME by lazy {
    if (SystemInfo.isWindows) {
      return@lazy "java.exe"
    }
    return@lazy "java"
  }

  open val fileName: String by lazy {
    initialLocation.name
  }

  open val installationPath: File by lazy {
    //todo change the filename to a composite name from version
    File(BinTrayUtil.getJdkStoragePathFile(), BinTrayUtil.archveToDirectoryName(fileName))
  }

  val transitionPath: File by lazy {
    File(PathManager.getPluginTempPath(), fileName)
  }

  val downloadPath: File by lazy {
    File(BinTrayUtil.downloadPath(), fileName)

  }

  abstract fun install()

  override fun toString(): String {
    return fileName
  }

  protected fun fetchVersion(): String? {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<String, RuntimeException>(
      {
        installationPath.walk().filter { file -> file.name == JAVA_FILE_NAME }.firstOrNull()?.let { javaFile ->
          try {
            val output = ExecUtil.execAndGetOutput(GeneralCommandLine(javaFile.path, "-version"))
            val matchResult: MatchResult? = "\\((build [^)]*)\\)".toRegex().find(output.stderr)
            matchResult?.groups?.get(1)?.value
          } catch (e: Exception) {
            println("tried to execute : ${javaFile.path}, \"-version\")")
            println("Error: ${e}")
            null
          }
        }
      },
      "Fetching Runtime Version", true, ProjectManagerEx.getInstanceEx().defaultProject)
  }
}