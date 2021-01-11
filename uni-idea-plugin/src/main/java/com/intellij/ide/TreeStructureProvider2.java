// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.ProjectExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Allows a plugin to modify the structure of a project as displayed in the project view.
 *
 * @see ProjectViewNodeDecorator
 */
public interface TreeStructureProvider2 {
  ProjectExtensionPointName<TreeStructureProvider2> EP = new ProjectExtensionPointName<>("com.intellij.treeStructureProvider");

  /**
   * @deprecated Use {@link #EP}
   */
  @Deprecated
  ExtensionPointName<TreeStructureProvider2> EP_NAME = ExtensionPointName.create("com.intellij.treeStructureProvider");

  /**
   * Allows a plugin to modify the list of children displayed for the specified node in the
   * project view.
   *
   * @param parent   the parent node.
   * @param children the list of child nodes according to the default project structure.
   *                 Elements of the collection are of type {@link ProjectViewNode}.
   * @param settings the current project view settings.
   * @return the modified collection of child nodes, or {@code children} if no modifications
   *         are required.
   */
  @NotNull
  Collection<AbstractTreeNod2<?>> modify(@NotNull AbstractTreeNod2<?> parent, @NotNull Collection<AbstractTreeNod2<?>> children, ViewSettings settings);

  /**
   * Returns a user data object of the specified type for the specified selection in the
   * project view.
   *
   * @param selected the list of nodes currently selected in the project view.
   * @param dataId the identifier of the requested data object (for example, as defined in
   *                 {@link com.intellij.openapi.actionSystem.PlatformDataKeys})
   * @return the data object, or null if no data object can be returned by this provider.
   * @see com.intellij.openapi.actionSystem.DataProvider
   */
  @Nullable
  default Object getData(@NotNull Collection<AbstractTreeNod2<?>> selected, @NotNull String dataId) {
    return null;
  }
}
