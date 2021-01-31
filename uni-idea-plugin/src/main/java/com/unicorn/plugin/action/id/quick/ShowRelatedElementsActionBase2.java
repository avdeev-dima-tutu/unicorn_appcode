// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.unicorn.plugin.action.id.quick;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.hint.*;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.navigation.BackgroundUpdaterTaskBase;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PopupAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.GenericListComponentUpdater;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.reference.SoftReference;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupPositionManager;
import com.intellij.ui.popup.PopupUpdateProcessor;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ShowRelatedElementsActionBase2 extends DumbAwareAction implements PopupAction {
  private Reference<JBPopup> myPopupRef;
  private Reference<ImplementationsUpdaterTask> myTaskRef;

  public ShowRelatedElementsActionBase2() {
    setEnabledInModalContext(true);
    setInjectedContext(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    performForContext(e, e.getDataContext(), true);
  }

  public void performForContext(AnActionEvent e, @NotNull DataContext dataContext, boolean invokedByShortcut) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return;
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    boolean isInvokedFromEditor = CommonDataKeys.EDITOR.getData(dataContext) != null;

    PsiElement psiElement = e.getRequiredData(CommonDataKeys.PSI_ELEMENT);
    VirtualFile virtualFile = Uni.INSTANCE.getSelectedFile();//e.getData(CommonDataKeys.VIRTUAL_FILE);
    PsiImplementationViewSession2 viewSession = new PsiImplementationViewSession2(project, psiElement, PsiElement.EMPTY_ARRAY, "todo", null, virtualFile, false, true);
    showImplementations(e, virtualFile, viewSession, isInvokedFromEditor, invokedByShortcut);

//    try {
//      for (ImplementationViewSessionFactory factory : getSessionFactories()) {
//        ImplementationViewSession session = factory.createSession(dataContext, project, isSearchDeep(), isIncludeAlwaysSelf());
//        if (session != null) {
//          showImplementations(e, session, isInvokedFromEditor, invokedByShortcut);
//        }
//      }
//    }
//    catch (IndexNotReadyException ex) {
//      DumbService.getInstance(project).showDumbModeNotification(getIndexNotReadyMessage());
//    }
  }

  @NotNull
  protected abstract List<ImplementationViewSessionFactory> getSessionFactories();

  @NotNull
  protected abstract @NlsContexts.PopupContent String getIndexNotReadyMessage();

  private void updateElementImplementations(AnActionEvent e, final Object lookupItemObject, ImplementationViewSession session) {
    ImplementationViewSessionFactory currentFactory = session.getFactory();

    PsiElement psiElement = e.getRequiredData(CommonDataKeys.PSI_ELEMENT);
    Project project = e.getProject();
    VirtualFile virtualFile = Uni.INSTANCE.getSelectedFile();//e.getData(CommonDataKeys.VIRTUAL_FILE);
    PsiImplementationViewSession2 newSession = new PsiImplementationViewSession2(project, psiElement, PsiElement.EMPTY_ARRAY, "todo", null, virtualFile, false, true);
//    PsiImplementationViewSession2 newSession = createNewSession(currentFactory, session, lookupItemObject);
//    if (newSession == null) {
//      for (ImplementationViewSessionFactory factory : getSessionFactories()) {
//        if (currentFactory == factory) continue;
//        newSession = createNewSession(factory, session, lookupItemObject);
//        if (newSession != null) break;
//      }
//    }
//    if (newSession != null) {
//      Disposer.dispose(session);//todo
      showImplementations(e, virtualFile, newSession, false, false);
//    }
  }

//  private PsiImplementationViewSession2 createNewSession(ImplementationViewSessionFactory factory,
//                                                     ImplementationViewSession session,
//                                                     Object lookupItemObject) {
//    return factory.createSessionForLookupElement(session.getProject(), session.getEditor(), session.getFile(), lookupItemObject,
//                                                 isSearchDeep(), isIncludeAlwaysSelf());
//  }

  protected void showImplementations(AnActionEvent e,
                                     @NotNull VirtualFile virtualFile,
                                     @NotNull PsiImplementationViewSession2 session,
                                     boolean invokedFromEditor,
                                     boolean invokedByShortcut) {

    PsiElement psiElement = e.getRequiredData(CommonDataKeys.PSI_ELEMENT);
    List<ImplementationViewElement> impls = List.of(new PsiImplementationViewElement(psiElement));//session.getImplementationElements();

    Project project = e.getProject();
    triggerFeatureUsed(project);
    int index = 0;
    if (invokedFromEditor && virtualFile != null && impls.size() > 1) {
      final VirtualFile containingFile = impls.get(0).getContainingFile();
      if (virtualFile.equals(containingFile)) {
        final VirtualFile secondContainingFile = impls.get(1).getContainingFile();
        if (secondContainingFile != null && !secondContainingFile.equals(containingFile)) {
          index = 1;
        }
      }
    }

    final Ref<UsageView> usageView = new Ref<>();
    final String title = "popup_title: " + virtualFile.getName();
    JBPopup popup = SoftReference.dereference(myPopupRef);
    if (popup != null && popup.isVisible() && popup instanceof AbstractPopup) {
      final ImplementationViewComponent component = (ImplementationViewComponent)((AbstractPopup)popup).getComponent();
      popup.setCaption(title);
      component.update(impls, index);
      updateInBackground(session, component, title, (AbstractPopup)popup, usageView);
      if (invokedByShortcut) {
        ((AbstractPopup)popup).focusPreferredComponent();
      }
      return;
    }

    final ImplementationViewComponent component = new ImplementationViewComponent(impls, index);
    if (component.hasElementsToShow()) {
      final PopupUpdateProcessor updateProcessor = new PopupUpdateProcessor(project) {
        @Override
        public void updatePopup(Object lookupItemObject) {
          updateElementImplementations(e, lookupItemObject, session);
        }
      };

      ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(component, component.getPreferredFocusableComponent())
        .setProject(project)
        .addListener(updateProcessor)
        .addUserData(updateProcessor)
        .setDimensionServiceKey(project, DocumentationManager.JAVADOC_LOCATION_AND_SIZE, false)
        .setResizable(true)
        .setMovable(true)
        .setRequestFocus(invokedFromEditor && LookupManager.getActiveLookup(session.getEditor()) == null)
        .setTitle(title)
        .setCancelCallback(() -> {
          ImplementationsUpdaterTask task = SoftReference.dereference(myTaskRef);
          if (task != null) {
            task.cancelTask();
          }
          Disposer.dispose(session);
          return Boolean.TRUE;
        });
      if (couldPinPopup()) {
        popupBuilder.setCouldPin(popup1 -> {
          usageView.set(component.showInUsageView());
          popup1.cancel();
          myTaskRef = null;
          return false;
        });
      }
      popup = popupBuilder.createPopup();

      updateInBackground(session, component, title, (AbstractPopup)popup, usageView);

      PopupPositionManager.positionPopupInBestPosition(popup, session.getEditor(), DataManager.getInstance().getDataContext());
      component.setHint(popup, title);

      myPopupRef = new WeakReference<>(popup);
    }
  }

  protected abstract void triggerFeatureUsed(@NotNull Project project);

  protected static void triggerFeatureUsed(@NotNull Project project, @NotNull String key, @NotNull String keyForLookup) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed(key);
    if (LookupManager.getInstance(project).getActiveLookup() != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(keyForLookup);
    }
  }

  @NotNull
  protected abstract @NlsContexts.PopupTitle String getPopupTitle(@NotNull ImplementationViewSession session);

  protected abstract boolean couldPinPopup();

  private void updateInBackground(@NotNull ImplementationViewSession session,
                                  @NotNull ImplementationViewComponent component,
                                  @NlsContexts.PopupTitle String title,
                                  @NotNull AbstractPopup popup,
                                  @NotNull Ref<? extends UsageView> usageView) {
    final ImplementationsUpdaterTask updaterTask = SoftReference.dereference(myTaskRef);
    if (updaterTask != null) {
      updaterTask.cancelTask();
    }

    if (!session.needUpdateInBackground()) return;  // already found
    final ImplementationsUpdaterTask task = new ImplementationsUpdaterTask(session, title, component);
    task.init(popup, new ImplementationViewComponentUpdater(component, session.elementRequiresIncludeSelf() ? 1 : 0), usageView);

    myTaskRef = new WeakReference<>(task);
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new BackgroundableProcessIndicator(task));
  }

  protected boolean isIncludeAlwaysSelf() {
    return true;
  }

  protected boolean isSearchDeep() {
    return false;
  }

  private static class ImplementationViewComponentUpdater implements GenericListComponentUpdater<ImplementationViewElement> {
    private final ImplementationViewComponent myComponent;
    private final int myIncludeSelfIdx;

    ImplementationViewComponentUpdater(ImplementationViewComponent component, int includeSelfIdx) {
      myComponent = component;
      myIncludeSelfIdx = includeSelfIdx;
    }

    @Override
    public void paintBusy(boolean paintBusy) {
      //todo notify busy
    }

    @Override
    public void replaceModel(@NotNull List<? extends ImplementationViewElement> data) {
      final ImplementationViewElement[] elements = myComponent.getElements();
      final int startIdx = elements.length - myIncludeSelfIdx;
      List<ImplementationViewElement> result = new ArrayList<>();
      Collections.addAll(result, elements);
      result.addAll(data.subList(startIdx, data.size()));
      myComponent.update(result, myComponent.getIndex());
    }
  }

  private static final class ImplementationsUpdaterTask extends BackgroundUpdaterTaskBase<ImplementationViewElement> {
    private final @NlsContexts.PopupTitle String myCaption;
    private final ImplementationViewSession mySession;
    private final ImplementationViewComponent myComponent;
    private List<ImplementationViewElement> myElements;

    private ImplementationsUpdaterTask(ImplementationViewSession session,
                                       final @NlsContexts.PopupTitle String caption,
                                       ImplementationViewComponent component) {
      super(session.getProject(), ImplementationSearcher.getSearchingForImplementations(), null);
      myCaption = caption;
      mySession = session;
      myComponent = component;
    }

    @Override
    public String getCaption(int size) {
      return myCaption;
    }

    @Override
    protected Usage createUsage(ImplementationViewElement element) {
      return element.getUsage();
    }


    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
      super.run(indicator);
      myElements = mySession.searchImplementationsInBackground(indicator, this::updateComponent);
    }

    @Override
    public int getCurrentSize() {
      if (myElements != null) return myElements.size();
      return super.getCurrentSize();
    }

    @Override
    public void onSuccess() {
      if (!cancelTask()) {
        myComponent.update(myElements, myComponent.getIndex());
      }
      super.onSuccess();
    }
  }
}
