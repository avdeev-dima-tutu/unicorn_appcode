/*
 * Copyright (c) 2019 Patrick Scheibe, Dmitry Kashin, Athiele.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.halirutan.keypromoterx2

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Core class to create the Key Promoter X tool-window. Nothing interesting here except of template code.
 *
 * @author athiele, Patrick Scheibe
 */
class KeyPromoterToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun isApplicable(project: Project): Boolean {
    return ConfKeyPromoter.KEY_PROMOTER_AVAILABLE
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    return ConfKeyPromoter.KEY_PROMOTER_AVAILABLE
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val toolWindowBuilder = KeyPromoterToolWindowPanel()
    val contentFactory = ContentFactory.SERVICE.getInstance()
    val toolWindowContent = toolWindowBuilder.content
    val content = contentFactory.createContent(toolWindowContent, "", false)
    content.preferredFocusableComponent = toolWindowContent
    content.setDisposer(toolWindowBuilder)
    toolWindow.contentManager.addContent(content)
  }
}