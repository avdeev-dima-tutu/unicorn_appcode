// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.tutu.idea.file;

import com.intellij.ide.util.treeView.AbstractTreeUi;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.ui.*;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.WideSelectionTreeUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
public abstract class ColoredTreeCellRenderer2 extends SimpleColoredComponent implements TreeCellRenderer {
  public static final Logger LOG = Logger.getInstance(ColoredTreeCellRenderer2.class);

  public static final Icon LOADING_NODE_ICON = JBUIScale.scaleIcon(EmptyIcon.create(8, 16));

  /**
   * Defines whether the tree is selected or not
   */
  public boolean mySelected;
  /**
   * Defines whether the tree has focus or not
   */
  public boolean myFocused;
  public boolean myFocusedCalculated;

  public boolean myUsedCustomSpeedSearchHighlighting = false;

  public JTree myTree;

  public boolean myOpaque = true;

  @Override
  public final Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
    try {
      rendererComponentInner(tree, value, selected, expanded, leaf, row, hasFocus);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      try { LOG.error(e); } catch (Exception ignore) { }
    }
    return this;
  }

  @SuppressWarnings("UnstableApiUsage")
  public abstract void rendererComponentInner(JTree tree,
                                              Object value,
                                              boolean selected,
                                              boolean expanded,
                                              boolean leaf,
                                              int row,
                                              boolean hasFocus);

  public JTree getTree() {
    return myTree;
  }

  public final boolean isFocused() {
    if (!myFocusedCalculated) {
      myFocused = calcFocusedState();
      myFocusedCalculated = true;
    }
    return myFocused;
  }

  public boolean calcFocusedState() {
    return myTree.hasFocus();
  }

  @Override
  public void setOpaque(boolean isOpaque) {
    myOpaque = isOpaque;
    super.setOpaque(isOpaque);
  }

  @Override
  public Font getFont() {
    Font font = super.getFont();

    // Cell renderers could have no parent and no explicit set font.
    // Take tree font in this case.
    if (font != null) return font;
    JTree tree = getTree();
    return tree != null ? tree.getFont() : null;
  }

  /**
   * When the item is selected then we use default tree's selection foreground.
   * It guaranties readability of selected text in any LAF.
   */
  @Override
  public void append(@NotNull @Nls String fragment, @NotNull SimpleTextAttributes attributes, boolean isMainText) {
    if (mySelected && isFocused()) {
      super.append(fragment, new SimpleTextAttributes(attributes.getStyle(), UIUtil.getTreeSelectionForeground(true)), isMainText);
    }
    else {
      super.append(fragment, attributes, isMainText);
    }
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleColoredTreeCellRenderer();
    }
    return accessibleContext;
  }

  public class AccessibleColoredTreeCellRenderer extends AccessibleSimpleColoredComponent {
  }

  // The following method are overridden for performance reasons.
  // See the Implementation Note for more information.
  // javax.swing.tree.DefaultTreeCellRenderer
  // javax.swing.DefaultListCellRenderer

  @Override
  public void validate() {
  }

  @Override
  public void invalidate() {
  }

  @Override
  public void revalidate() {
  }

  @Override
  public void repaint(long tm, int x, int y, int width, int height) {
  }

  @Override
  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, char oldValue, char newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, short oldValue, short newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, int oldValue, int newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, long oldValue, long newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, float oldValue, float newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, double oldValue, double newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
  }
}
