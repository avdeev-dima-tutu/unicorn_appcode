// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package bootRuntime2.command;

import bootRuntime2.main.BinTrayUtil;
import bootRuntime2.main.Controller;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import bootRuntime2.bundles.Runtime;

public class Download extends RuntimeCommand {

  private static final Logger LOG = Logger.getInstance("#com.intellij.bootRuntime.command.Download");

  Download(Project project, Controller controller, Runtime runtime) {
    super(project,controller,"Download", runtime);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    File downloadDirectoryFile = myRuntime.getDownloadPath();
    if (!BinTrayUtil.downloadPath().exists()) {
      BinTrayUtil.downloadPath().mkdir();
    }
    if (!downloadDirectoryFile.exists()) {
      String link = "https://bintray.com/jetbrains/intellij-jbr/download_file?file_path=" + getRuntime().getFileName();
      String oldLink = "https://bintray.com/jetbrains/intellij-jdk/download_file?file_path=" + getRuntime().getFileName();

    runWithProgress("Downloading...", true, (progressIndicator) -> {
      progressIndicator.setIndeterminate(true);

      try {
        try {
          HttpRequests.request(link).saveToFile(downloadDirectoryFile, progressIndicator);
        }
        catch (HttpRequests.HttpStatusException ioe) {
          HttpRequests.request(oldLink).saveToFile(downloadDirectoryFile, progressIndicator);
        }
      } catch (IOException ex) {
        LOG.warn(ex);
      }
    });
  }
}
}
