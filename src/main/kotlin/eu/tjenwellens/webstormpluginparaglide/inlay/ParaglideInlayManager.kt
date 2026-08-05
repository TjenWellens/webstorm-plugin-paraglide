package eu.tjenwellens.webstormpluginparaglide.inlay

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

class ParaglideInlayManager {

    private val inlays = mutableListOf<Inlay<*>>()

    fun clear() {
        inlays.forEach { it.dispose() }
        inlays.clear()
    }

    fun addTranslation(
        editor: Editor,
        offset: Int,
        text: String
    ) {
        val inlay = editor.inlayModel.addInlineElement(
            offset,
            true,
            object : EditorCustomElementRenderer {

                override fun calcWidthInPixels(
                    inlay: Inlay<*>
                ): Int {
                    return editor.contentComponent
                        .getFontMetrics(editor.contentComponent.font)
                        .stringWidth("  $text")
                }

                override fun paint(
                    inlay: Inlay<*>,
                    g: Graphics,
                    targetRegion: Rectangle,
                    textAttributes: TextAttributes
                ) {
                    g.font = editor.contentComponent.font
                        .deriveFont(Font.ITALIC)

                    g.drawString(
                        "  $text",
                        targetRegion.x,
                        targetRegion.y +
                                editor.ascent
                    )
                }
            }
        )

        if (inlay != null) {
            inlays.add(inlay)
        }
    }
}