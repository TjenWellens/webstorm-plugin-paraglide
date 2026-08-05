package eu.tjenwellens.webstormpluginparaglide.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiReferenceContributor
import eu.tjenwellens.webstormpluginparaglide.services.TranslationIndex

class TranslationStartupActivity : ProjectActivity {
    private val log = Logger.getInstance(TranslationStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        log.info("Paraglide: building translation index")

        TranslationIndex
            .getInstance(project)
            .rebuild()

        PsiReferenceContributor.EP_NAME.extensionList
            .forEach {
                log.warn(
                    "REFERENCE CONTRIBUTOR: ${it.javaClass.name}"
                )
            }
    }
}