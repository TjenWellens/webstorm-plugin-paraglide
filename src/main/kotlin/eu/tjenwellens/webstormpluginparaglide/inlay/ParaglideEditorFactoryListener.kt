package eu.tjenwellens.webstormpluginparaglide.inlay

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiRecursiveElementVisitor
import eu.tjenwellens.webstormpluginparaglide.services.TranslationIndex

class ParaglideEditorFactoryListener :
    EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor

        val project = getProject(editor)
            ?: return

        scan(editor, project)
    }

    private fun getProject(editor: Editor): Project? {
        val virtualFile =
            FileDocumentManager
                .getInstance()
                .getFile(editor.document)
                ?: return null

        return com.intellij.openapi.project.ProjectManager
            .getInstance()
            .openProjects
            .firstOrNull {
                virtualFile.fileSystem
                    .equals(it.baseDir?.fileSystem)
            }
    }

    private fun scan(
        editor: Editor,
        project: Project
    ) {
        val psiFile =
            PsiDocumentManager
                .getInstance(project)
                .getPsiFile(editor.document)
                ?: return

        val manager = ParaglideInlayManager()

        psiFile.accept(
            object : PsiRecursiveElementVisitor() {

                override fun visitElement(element: com.intellij.psi.PsiElement) {

                    if (element is JSCallExpression) {

                        val text = element.text

                        if (text.startsWith("m.") &&
                            text.endsWith("()")
                        ) {

                            val key =
                                text
                                    .removePrefix("m.")
                                    .removeSuffix("()")

                            val translation =
                                TranslationIndex
                                    .getInstance(project)
                                    .defaultTranslation(key)

                            if (translation != null) {
                                manager.addTranslation(
                                    editor,
                                    element.textRange.endOffset,
                                    translation
                                )
                            }
                        }
                    }

                    super.visitElement(element)
                }
            }
        )
    }
}