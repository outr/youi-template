package io.youi.template

import java.io.File

import com.outr.hookup.{Hookup, HookupServer}
import io.youi.app.ServerApplication
import io.youi.http.content.Content
import io.youi.server.handler.LanguageSupport
import io.youi.stream._
import profig.Profig

class ServerTemplateApplication(compiler: TemplateCompiler) extends ServerApplication with TemplateApplication {
  val languageSupport = new LanguageSupport()

  val hookup: HookupServer[TemplateHookup] = Hookup.server[TemplateHookup]

  addTemplate(
    lookup = (fileName: String) => {
      val file = new File(compiler.destinationDirectory, fileName)
      if (file.isFile) {
        Some(Content.file(file))
      } else {
        None
      }
    },
    excludeDotHTML = compiler.removeDotHTML,
    deltas = List(
      Delta.InsertFirstChild(ByTag("body"), s"""<input type="hidden" id="template_pages" value="${compiler.pages.mkString(";")}"/>""")
    )
  )
  handlers += languageSupport

  override def main(args: Array[String]): Unit = {
    Profig.loadDefaults()
    Profig.merge(args)
    start()
  }
}