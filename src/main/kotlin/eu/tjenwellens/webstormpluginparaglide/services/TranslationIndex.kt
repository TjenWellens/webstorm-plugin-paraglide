package eu.tjenwellens.webstormpluginparaglide.services

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class TranslationIndex(
    private val project: Project
) {
    private val log = Logger.getInstance(TranslationIndex::class.java)
    private val translationRoots = setOf(
        "translate",
        "messages",
        "translations"
    )
    private val defaultLocale = "en" // todo: read from paraglide settings

    data class TranslationValue(
        val locale: String,
        val text: String,
        val file: VirtualFile,
        val offset: Int
    )

    data class TranslationEntry(
        val key: String,
        val values: MutableMap<String, TranslationValue> = mutableMapOf()
    )

    private val translations = ConcurrentHashMap<String, TranslationEntry>()

    companion object {
        fun getInstance(project: Project): TranslationIndex =
            project.getService(TranslationIndex::class.java)
    }

    private fun findTranslationRoots(): List<VirtualFile> {
        val roots = mutableListOf<VirtualFile>()

        ProjectFileIndex.getInstance(project)
            .iterateContent { file ->
                if (file.isDirectory && file.name in translationRoots) {
                    roots.add(file)
                }
                true
            }

        return roots
    }

    private fun visitObject(
        obj: JsonObject,
        prefix: String,
        locale: String
    ) {
        for (property in obj.propertyList) {

            val key =
                if (prefix.isEmpty())
                    property.name
                else
                    "$prefix.${property.name}"

            when (val value = property.value) {

                is JsonObject -> {
                    visitObject(
                        value,
                        key,
                        locale
                    )
                }

                is JsonStringLiteral -> {
                    val entry =
                        translations.computeIfAbsent(key) {
                            TranslationEntry(key)
                        }

                    entry.values[locale] =
                        TranslationValue(
                            locale = locale,
                            text = value.value,
                            file = property.containingFile.virtualFile,
                            offset = property.nameElement.textOffset
                        )
                }
            }
        }
    }

    fun rebuild() {
        translations.clear()

        val psiManager = PsiManager.getInstance(project)

        findTranslationRoots().forEach { translationRoot ->

            VfsUtilCore.iterateChildrenRecursively(
                translationRoot,
                null
            ) { virtualFile ->

                if (virtualFile.extension != "json") {
                    return@iterateChildrenRecursively true
                }

                ReadAction.run<RuntimeException> {

                    val psiFile = psiManager.findFile(virtualFile)

                    if (psiFile !is JsonFile) {
                        return@run
                    }

                    val locale = virtualFile.nameWithoutExtension

                    val root = psiFile.topLevelValue as? JsonObject
                        ?: return@run

                    visitObject(
                        root,
                        "",
                        locale
                    )
                }

                true
            }
        }

        // todo: remove logging
        log.info("Translation keys found:")
        translations.keys.forEach { key ->
            log.info(key)
        }

        fun lookup(key: String): TranslationEntry? =
            translations[key]

        fun allKeys(): Set<String> =
            translations.keys

        fun contains(key: String): Boolean =
            translations.containsKey(key)

        fun defaultTranslation(
            key: String,
            defaultLocale: String = "en"
        ): String? =
            translations[key]
                ?.values
                ?.get(defaultLocale)
                ?.text

        fun clear() {
            translations.clear()
        }
    }

    fun defaultTranslation(key: String): String? {
        val entry = translations[key] ?: return null

        return entry.values[defaultLocale]?.text
    }

}