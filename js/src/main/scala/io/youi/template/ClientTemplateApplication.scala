package io.youi.template

import com.outr.hookup.Hookup
import io.youi.activate.ActivationSupport
import io.youi.app.ClientApplication
import io.youi.dom
import io.youi.net.Path
import org.scalajs.dom._

import scala.scalajs.js.annotation.JSExportTopLevel

object ClientTemplateApplication extends TemplateApplication with ClientApplication {
  private lazy val pages = dom.byId[html.Input]("template_pages").value.split(';').toSet

  val hookup: TemplateHookup = Hookup.client[TemplateHookup]

  @JSExportTopLevel("application")
  def main(): Unit = {
    ActivationSupport.debug = true
    val paths = pages.map { page =>
      s"/${page.substring(0, page.indexOf('.'))}"
    }
    paths.map { p =>
      new TemplateScreen(Path.parse(p))
    }

    println("Template initialized!")
  }
}