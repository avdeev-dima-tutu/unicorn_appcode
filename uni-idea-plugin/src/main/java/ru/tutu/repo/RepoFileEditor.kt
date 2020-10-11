package ru.tutu.repo

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.panel
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class RepoFileEditor(private val project: Project, virtualFile: VirtualFile): FileEditor {
    val viewPanel = panel {
      row {
        button("repo update") {
          println("--------repo.json--------")
          println(String(virtualFile.inputStream.readBytes()))
          //todo
        }
      }
    }

    init {
//        Disposer.register(this, viewPanel)//todo

//        viewPanel.addPageChangeListener {
//            notifyPageChanged(it)
//        }
//        notifyPageChanged(pageState)
    }

//    private fun notifyPageChanged(pageState: DocumentPageState) {
//        project.messageBus.syncPublisher(DocumentPageStateListener.DOCUMENT_PAGE_STATE).run {
//            pageStateChanged(pageState)
//        }
//    }

//    fun reloadDocument() = viewPanel.reloadDocument()
//    fun increaseScale() = viewPanel.increaseScale()
//    fun decreaseScale() = viewPanel.decreaseScale()
//    fun nextPage() = viewPanel.nextPage()
//    fun previousPage() = viewPanel.previousPage()
//    fun findNext() = viewPanel.findNext()
//    fun findPrevious() = viewPanel.findPrevious()

//    val pageState
//        get() = viewPanel.run {
//            DocumentPageState(currentPageNumber, pagesCount)
//        }

    override fun isModified(): Boolean = false

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun getName(): String = NAME

    override fun setState(state: FileEditorState) {
//        if (state !is PdfFileEditorState) {
//            return
//        }
//        viewPanel.currentPageNumber = state.page
    }

//    override fun getState(level: FileEditorStateLevel): FileEditorState {
//        return PdfFileEditorState(viewPanel.currentPageNumber)
//    }

    override fun getComponent(): JComponent = viewPanel

    override fun getPreferredFocusedComponent(): JComponent? = viewPanel

    override fun <T : Any?> getUserData(key: Key<T>): T? = null

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = Unit

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun isValid(): Boolean = true

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun dispose() = Unit

    companion object {
        private const val NAME = "Pdf Viewer File Editor"
    }
}
