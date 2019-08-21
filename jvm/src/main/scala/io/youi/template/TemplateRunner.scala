package io.youi.template

import java.io.File

import profig.Profig
import scribe.Logger
import scribe.format._

import scala.io.StdIn

object TemplateRunner {
  def main(args: Array[String]): Unit = {
    Profig.loadDefaults()
    Profig.merge(args)

    Logger.root
      .clearHandlers()
      .withHandler(formatter = formatter"$date - $message$newLine")
      .replace()
    val config = Profig.as[TemplateConfig]
    assert(config.source.nonEmpty, "Source path must be specified (-source).")
    val source = new File(config.source.get)
    assert(source.isDirectory, s"Source directory must be a directory (${source.getAbsolutePath})")
    assert(config.destination.nonEmpty, "Destination path must be specified (-destination).")
    val destination = new File(config.destination.get)
    assert(!destination.isFile, s"Destination must be a directory, but found a file (${destination.getAbsolutePath})")
    destination.mkdirs()
    val optimize = config.modification == "optimize"

    val compiler = new TemplateCompiler(source, destination, removeDotHTML = config.removeDotHTML, consoleCommands = true, optimize = optimize)
    try {
      compiler.compileAll(deleteFirst = true)
      if (config.mode.equalsIgnoreCase("watch") || config.mode.equalsIgnoreCase("server")) {
        compiler.watch()
      }
      if (config.mode.equalsIgnoreCase("server")) {
        compiler.startServer()
      }
      if (config.mode.equalsIgnoreCase("watch") || config.mode.equalsIgnoreCase("server")) {
        println("Press ENTER on your keyboard to stop...")
        StdIn.readLine()
        println("Shutting down...")
        compiler.stopWatching()
        if (config.mode.equalsIgnoreCase("server")) {
          compiler.stopServer()
        }
        sys.exit(0)
      } else {
        sys.exit(0)
      }
    } catch {
      case t: Throwable => {
        scribe.error(t)
        compiler.stopWatching()
        compiler.stopServer()
        System.exit(0)
      }
    }
  }
}

case class TemplateConfig(mode: String = "compile",
                          modification: String = "none",
                          source: Option[String],
                          destination: Option[String],
                          removeDotHTML: Boolean = false)