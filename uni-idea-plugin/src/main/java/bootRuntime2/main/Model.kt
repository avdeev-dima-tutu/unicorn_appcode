// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package bootRuntime2.main

import bootRuntime2.bundles.Local
import bootRuntime2.bundles.Remote
import com.intellij.openapi.util.io.FileUtil
import bootRuntime2.bundles.Runtime

enum class BundleState {
  REMOTE,
  LOCAL,
  DOWNLOADED,
  EXTRACTED,
  INSTALLED,
  UNINSTALLED
}

class Model(var selectedBundle: Runtime, val bundles:MutableList<Runtime>) {

  fun updateBundle(newBundle:Runtime) {
    selectedBundle = newBundle
  }

  fun currentState () : BundleState {
     return when {
       isInstalled(selectedBundle) -> BundleState.INSTALLED
       isLocal(selectedBundle) -> BundleState.LOCAL
       isExtracted(selectedBundle) -> BundleState.EXTRACTED
       isDownloaded(selectedBundle) -> BundleState.DOWNLOADED
       isRemote(selectedBundle) -> BundleState.REMOTE
       else -> BundleState.UNINSTALLED
     }
  }

  fun isInstalled(bundle:Runtime):Boolean = bundle.installationPath.exists() &&
          BinTrayUtil.getJdkConfigFilePath().exists() &&
          FileUtil.loadFile(BinTrayUtil.getJdkConfigFilePath()).startsWith(bundle.installationPath.absolutePath)

  private fun isExtracted(bundle:Runtime):Boolean = bundle.installationPath.exists()

  private fun isDownloaded(bundle:Runtime):Boolean = bundle.downloadPath.exists()

  fun isRemote(bundle:Runtime):Boolean = bundle is Remote

  fun isLocal(bundle:Runtime):Boolean = bundle is Local
}
