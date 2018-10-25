package io.youi.template

import java.io.File

import io.youi.app.ServerApplication
import io.youi.http.Content
import io.youi.stream._
import profig.Profig

class ServerTemplateApplication(compiler: TemplateCompiler) extends ServerApplication with TemplateApplication {
  addTemplate(
    lookup = (fileName: String) => {
      val file = new File(compiler.destinationDirectory, fileName)
      if (file.exists()) {
        Some(Content.file(file))
      } else {
        None
      }
    },
    deltas = List(
      Delta.InsertFirstChild(ByTag("body"), s"""<input type="hidden" id="template_pages" value="${compiler.pages.mkString(";")}"/>""")
    )
  )

  override def main(args: Array[String]): Unit = {
    Profig.loadDefaults()
    Profig.merge(args)
    start()
  }
}