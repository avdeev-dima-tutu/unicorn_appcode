// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.*;
import com.intellij.ide.dnd.*;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.impl.FlattenModulesToggleAction;
import com.intellij.ide.projectView.*;
import com.intellij.ide.projectView.impl.nodes.AbstractModuleNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.*;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.TreePathUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.unicorn.Uni;
import org.jetbrains.annotations.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.BooleanSupplier;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractProjectViewPane2 {
  public final @NotNull Project myProject;
  @NotNull public DnDAwareTree myTree;
  @NotNull public ProjectAbstractTreeStructureBase myTreeStructure;
  @Nullable public DnDTarget myDropTarget;
  @Nullable public DnDSource myDragSource;

  public AbstractProjectViewPane2(@NotNull Project project) {
    myProject = project;
    Disposable fileManagerDisposable = new Disposable() {
      @Override
      public void dispose() {
        if (myDropTarget != null) {
          DnDManager.getInstance().unregisterTarget(myDropTarget, myTree);
          myDropTarget = null;
        }
        if (myDragSource != null) {
          DnDManager.getInstance().unregisterSource(myDragSource, myTree);
          myDragSource = null;
        }
      }
    };
    Disposer.register(
      Uni.INSTANCE,
      fileManagerDisposable
    );
  }

  public abstract @NotNull String getId();

  public TreePath[] getSelectionPaths() {
    return myTree.getSelectionPaths();
  }

  /**
   * @deprecated added in {@link ProjectViewImpl} automatically
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
  public ToggleAction createFlattenModulesAction(@NotNull BooleanSupplier isApplicable) {
    return new FlattenModulesToggleAction(myProject, () -> isApplicable.getAsBoolean() && ProjectView.getInstance(myProject).isShowModules(getId()),
                                          () -> ProjectView.getInstance(myProject).isFlattenModules(getId()),
                                          value -> ProjectView.getInstance(myProject).setFlattenModules(getId(), value));
  }

  public final TreePath getSelectedPath() {
    return TreeUtil.getSelectedPathIfOne(myTree);
  }

  /**
   * @see TreeUtil#getUserObject(Object)
   * @deprecated AbstractProjectViewPane#getSelectedPath
   */
  @Deprecated
  public final DefaultMutableTreeNode getSelectedNode() {
    TreePath path = getSelectedPath();
    return path == null ? null : ObjectUtils.tryCast(path.getLastPathComponent(), DefaultMutableTreeNode.class);
  }

  public final PsiElement @NotNull [] getSelectedPSIElements() {
    TreePath[] paths = getSelectionPaths();
    if (paths == null) return PsiElement.EMPTY_ARRAY;
    List<PsiElement> result = new ArrayList<>();
    for (TreePath path : paths) {
      result.addAll(getElementsFromNode(path.getLastPathComponent()));
    }
    return PsiUtilCore.toPsiElementArray(result);
  }

  public @Nullable PsiElement getFirstElementFromNode(@Nullable Object node) {
    return ContainerUtil.getFirstItem(getElementsFromNode(node));
  }

  @NotNull
  public List<PsiElement> getElementsFromNode(@Nullable Object node) {
    Object value = getValueFromNode(node);
    JBIterable<?> it = value instanceof PsiElement || value instanceof VirtualFile ? JBIterable.of(value) :
                       value instanceof Object[] ? JBIterable.of((Object[])value) :
                       value instanceof Iterable ? JBIterable.from((Iterable<?>)value) :
                       JBIterable.of(TreeUtil.getUserObject(node));
    return it.flatten(o -> o instanceof RootsProvider ? ((RootsProvider)o).getRoots() : Collections.singleton(o))
      .map(o -> o instanceof VirtualFile ? PsiUtilCore.findFileSystemItem(myProject, (VirtualFile)o) : o)
      .filter(PsiElement.class)
      .filter(PsiElement::isValid)
      .toList();
  }

  /** @deprecated use {@link AbstractProjectViewPane2#getElementsFromNode(Object)}**/
  @Deprecated
  @Nullable
  public PsiElement getPSIElementFromNode(@Nullable TreeNode node) {
    return getFirstElementFromNode(node);
  }

  @Nullable
  public Module getNodeModule(@Nullable final Object element) {
    if (element instanceof PsiElement) {
      PsiElement psiElement = (PsiElement)element;
      return ModuleUtilCore.findModuleForPsiElement(psiElement);
    }
    return null;
  }

  public final Object @NotNull [] getSelectedElements() {
    TreePath[] paths = getSelectionPaths();
    if (paths == null) return PsiElement.EMPTY_ARRAY;
    ArrayList<Object> list = new ArrayList<>(paths.length);
    for (TreePath path : paths) {
      Object lastPathComponent = path.getLastPathComponent();
      Object element = getValueFromNode(lastPathComponent);
      if (element instanceof Object[]) {
        Collections.addAll(list, (Object[])element);
      }
      else if (element != null) {
        list.add(element);
      }
    }
    return ArrayUtil.toObjectArray(list);
  }

  @Nullable
  public Object getValueFromNode(@Nullable Object node) {
    return extractValueFromNode(node);
  }

  /** @deprecated use {@link AbstractProjectViewPane2#getValueFromNode(Object)} **/
  @Deprecated
  public Object exhumeElementFromNode(DefaultMutableTreeNode node) {
    return getValueFromNode(node);
  }

  @Nullable
  public static Object extractValueFromNode(@Nullable Object node) {
    Object userObject = TreeUtil.getUserObject(node);
    Object element = null;
    if (userObject instanceof AbstractTreeNode) {
      AbstractTreeNode descriptor = (AbstractTreeNode)userObject;
      element = descriptor.getValue();
    }
    else if (userObject instanceof NodeDescriptor) {
      NodeDescriptor descriptor = (NodeDescriptor)userObject;
      element = descriptor.getElement();
      if (element instanceof AbstractTreeNode) {
        element = ((AbstractTreeNode)element).getValue();
      }
    }
    else if (userObject != null) {
      element = userObject;
    }
    return element;
  }

  public @NotNull TreeExpander createTreeExpander() {
    return new DefaultTreeExpander(this::getTree) {
      public boolean isExpandAllAllowed() {
        JTree tree = getTree();
        TreeModel model = tree.getModel();
        return model == null || model instanceof AsyncTreeModel || model instanceof InvokerSupplier;
      }

      @Override
      public boolean isExpandAllVisible() {
        return isExpandAllAllowed() && Registry.is("ide.project.view.expand.all.action.visible");
      }

      @Override
      public boolean canExpand() {
        return isExpandAllAllowed() && super.canExpand();
      }

      @Override
      public void collapseAll(@NotNull JTree tree, boolean strict, int keepSelectionLevel) {
        super.collapseAll(tree, false, keepSelectionLevel);
      }
    };
  }


  public @NotNull Comparator<NodeDescriptor<?>> createComparator() {
    return new GroupByTypeComparator(myProject, getId());
  }

  void installComparator(AbstractTreeBuilder treeBuilder) {
    installComparator(treeBuilder, createComparator());
  }

  public void installComparator(AbstractTreeBuilder builder, @NotNull Comparator<? super NodeDescriptor<?>> comparator) {
    if (builder != null) {
      builder.setNodeDescriptorComparator(comparator);
    }
  }

  @NotNull public JTree getTree() {
    return myTree;
  }

  public PsiDirectory @NotNull [] getSelectedDirectoriesInAmbiguousCase(Object userObject) {
    if (userObject instanceof AbstractModuleNode) {
      final Module module = ((AbstractModuleNode)userObject).getValue();
      if (module != null && !module.isDisposed()) {
        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        final VirtualFile[] sourceRoots = moduleRootManager.getSourceRoots();
        List<PsiDirectory> dirs = new ArrayList<>(sourceRoots.length);
        final PsiManager psiManager = PsiManager.getInstance(myProject);
        for (final VirtualFile sourceRoot : sourceRoots) {
          final PsiDirectory directory = psiManager.findDirectory(sourceRoot);
          if (directory != null) {
            dirs.add(directory);
          }
        }
        return dirs.toArray(PsiDirectory.EMPTY_ARRAY);
      }
    }
    else if (userObject instanceof ProjectViewNode) {
      VirtualFile file = ((ProjectViewNode)userObject).getVirtualFile();
      if (file != null && file.isValid() && file.isDirectory()) {
        PsiDirectory directory = PsiManager.getInstance(myProject).findDirectory(file);
        if (directory != null) {
          return new PsiDirectory[]{directory};
        }
      }
    }
    return PsiDirectory.EMPTY_ARRAY;
  }

  // Drag'n'Drop stuff

  public void enableDnD() {
    if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
      myDropTarget = new ProjectViewDropTarget2(myTree, myProject) {
        @Nullable
        @Override
        protected PsiElement getPsiElement(@NotNull TreePath path) {
          return getFirstElementFromNode(path.getLastPathComponent());
        }

        @Nullable
        @Override
        protected Module getModule(@NotNull PsiElement element) {
          return getNodeModule(element);
        }

        @Override
        public void cleanUpOnLeave() {
          super.cleanUpOnLeave();
        }

        @Override
        public boolean update(DnDEvent event) {
          return super.update(event);
        }
      };
      myDragSource = new MyDragSource();
      DnDManager dndManager = DnDManager.getInstance();
      dndManager.registerSource(myDragSource, myTree);
      dndManager.registerTarget(myDropTarget, myTree);
    }
  }

  public final class MyDragSource implements DnDSource {
    @Override
    public boolean canStartDragging(DnDAction action, Point dragOrigin) {
      if ((action.getActionId() & DnDConstants.ACTION_COPY_OR_MOVE) == 0) return false;
      final Object[] elements = getSelectedElements();
      final PsiElement[] psiElements = getSelectedPSIElements();
      DataContext dataContext = DataManager.getInstance().getDataContext(myTree);
      return psiElements.length > 0 || canDragElements(elements, dataContext, action.getActionId());
    }

    @Override
    public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin) {
      PsiElement[] psiElements = getSelectedPSIElements();
      TreePath[] paths = getSelectionPaths();
      return new DnDDragStartBean(new TransferableWrapper() {
        @Override
        public List<File> asFileList() {
          return PsiCopyPasteManager.asFileList(psiElements);
        }

        @Override
        public TreePath @Nullable [] getTreePaths() {
          return paths;
        }

        @Override
        public TreeNode[] getTreeNodes() {
          return TreePathUtil.toTreeNodes(getTreePaths());
        }

        @Override
        public PsiElement[] getPsiElements() {
          return psiElements;
        }
      });
    }

    // copy/paste from com.intellij.ide.dnd.aware.DnDAwareTree.createDragImage
    @Nullable
    @Override
    public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin, @NotNull DnDDragStartBean bean) {
      final TreePath[] paths = getSelectionPaths();
      if (paths == null) return null;

      List<Trinity<@Nls String, Icon, @Nullable VirtualFile>> toRender = new ArrayList<>();
      for (TreePath path : getSelectionPaths()) {
        Pair<Icon, @Nls String> iconAndText = getIconAndText(path);
        toRender.add(Trinity.create(iconAndText.second, iconAndText.first,
                                    PsiCopyPasteManager.asVirtualFile(getFirstElementFromNode(path.getLastPathComponent()))));
      }

      int count = 0;
      JPanel panel = new JPanel(new VerticalFlowLayout(0, 0));
      int maxItemsToShow = toRender.size() < 20 ? toRender.size() : 10;
      for (Trinity<@Nls String, Icon, @Nullable VirtualFile> trinity : toRender) {
        JLabel fileLabel = new DragImageLabel(trinity.first, trinity.second, trinity.third);
        panel.add(fileLabel);
        count++;
        if (count > maxItemsToShow) {
          panel.add(new DragImageLabel(IdeBundle.message("label.more.files", paths.length - maxItemsToShow), EmptyIcon.ICON_16, null));
          break;
        }
      }
      panel.setSize(panel.getPreferredSize());
      panel.doLayout();

      BufferedImage image = ImageUtil.createImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = (Graphics2D)image.getGraphics();
      panel.paint(g2);
      g2.dispose();

      return new Pair<>(image, new Point());
    }

    @NotNull
    public Pair<Icon, @Nls String> getIconAndText(TreePath path) {
      Object object = TreeUtil.getLastUserObject(path);
      Component component = getTree().getCellRenderer()
        .getTreeCellRendererComponent(getTree(), object, false, false, true, getTree().getRowForPath(path), false);
      Icon[] icon = new Icon[1];
      String[] text = new String[1];
      ObjectUtils.consumeIfCast(component, ProjectViewRenderer.class, renderer -> icon[0] = renderer.getIcon());
      ObjectUtils.consumeIfCast(component, SimpleColoredComponent.class, renderer -> text[0] = renderer.getCharSequence(true).toString());
      return Pair.create(icon[0], text[0]);
    }
  }

  public class DragImageLabel extends JLabel {
    public DragImageLabel(@Nls String text, Icon icon, @Nullable VirtualFile file) {
      super(text, icon, SwingConstants.LEADING);
      setFont(UIUtil.getTreeFont());
      setOpaque(true);
      if (file != null) {
        setBackground(EditorTabPresentationUtil.getEditorTabBackgroundColor(myProject, file, null));
        setForeground(EditorTabPresentationUtil.getFileForegroundColor(myProject, file));
      } else {
        setForeground(RenderingUtil.getForeground(getTree(), true));
        setBackground(RenderingUtil.getBackground(getTree(), true));
      }
      setBorder(new EmptyBorder(JBUI.CurrentTheme.EditorTabs.tabInsets()));
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      size.height = JBUI.scale(SingleHeightTabs.UNSCALED_PREF_HEIGHT);
      return size;
    }
  }

  public static boolean canDragElements(Object @NotNull [] elements, @NotNull DataContext dataContext, int dragAction) {
    for (Object element : elements) {
      if (element instanceof Module) {
        return true;
      }
    }
    return dragAction == DnDConstants.ACTION_MOVE && MoveHandler.canMove(dataContext);
  }

}
