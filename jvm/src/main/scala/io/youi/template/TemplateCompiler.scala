package io.youi.template

import java.io.File
import java.net.URI

import io.bit3.jsass.OutputStyle
import io.youi.optimizer.HTMLOptimizer
import io.youi.stream.{ByTag, Delta, HTMLParser}
import org.powerscala.io._
import org.powerscala.io.watcher.{PathEvent, Watcher}

import scala.collection.mutable.ListBuffer
import scala.sys.process._

class TemplateCompiler(val sourceDirectory: File,
                       val destinationDirectory: File,
                       val compressCSS: Boolean,
                       val removeDotHTML: Boolean,
                       val consoleCommands: Boolean,
                       val optimize: Boolean,
                       val spa: Boolean) {
  private[template] var pages = Set.empty[String]

  private val server = new ServerTemplateApplication(this)

  private val watcher = new Watcher(sourceDirectory.toPath, eventDelay = 3000L) {
    override def fire(pathEvent: PathEvent): Unit = try {
      val file = pathEvent.path.toAbsolutePath.toFile
      val path = file.getCanonicalPath.substring(sourceDirectory.getCanonicalPath.length)
      if (path.startsWith("/pages") && path.endsWith(".html")) {
        scribe.info(s"Page changed (${pathEvent.path}), recompiling...")
        compilePage(pathEvent.path.toFile)
      } else if (path.startsWith("/less") && path.endsWith(".less")) {
        scribe.info(s"LESS file changed (${file.getName}), recompiling all LESS files...")
        compileAllLess()
      } else if (path.startsWith("/sass") && (path.endsWith(".sass") || path.endsWith(".scss"))) {
        scribe.info(s"SASS file changed (${file.getName}), recompiling all SASS files...")
        compileAllSass()
      } else if (path.startsWith("/partials")) {
        scribe.info(s"Partial page changed (${file.getName}), recompiling all pages...")
        compileAllPages()
      } else {
        scribe.info(s"Unknown path: $path, recompiling everything...")
        compileAll(deleteFirst = false)
      }

      // Reload language cache
      server.languageSupport.clear()

      // Reload all active pages
      scribe.info("Compilation finished, reloading all clients...")
      server.hookup.all.foreach { hookup =>
        hookup.communication.reload(force = true)
      }
    } catch {
      case t: Throwable => scribe.error(t)
    }
  }

  def watch(): Unit = watcher.start()

  def stopWatching(): Unit = watcher.dispose()

  def startServer(): Unit = {
    server.start()
  }

  def stopServer(): Unit = server.stop()

  def compileAll(deleteFirst: Boolean): Unit = {
    // Delete output
    if (deleteFirst) {
      deleteDestination()
    }

    // Copy assets
    copyAssets()

    // Compile LESS
    compileAllLess()

    // Compile SASS files
    compileAllSass()

    // Generate pages
    compileAllPages()
  }

  private def processRecursively(directory: File)(handler: File => Unit): Unit = directory.listFiles.foreach { file =>
    if (file.isDirectory) {
      processRecursively(file)(handler)
    } else {
      handler(file)
    }
  }

  def compileAllPages(): Unit = {
    val pagesDirectory = new File(sourceDirectory, "pages")
    processRecursively(pagesDirectory) { file =>
      if (file.getName.endsWith(".html")) {
        compilePage(file)
      }
    }
  }

  def compilePage(fileName: String): Unit = {
    val source = new File(sourceDirectory, s"pages/$fileName")
    compilePage(source)
  }

  def compilePage(source: File): Unit = {
    val fileName = source.getAbsolutePath match {
      case s => s.substring(s.indexOf("pages/") + 6)
    }
    val destination = new File(destinationDirectory, fileName)
    val html = compileHTML(source).replaceAll("""\$\{.*?\}""", "")
    destination.getParentFile.mkdirs()
    IO.stream(html, destination)

    if (optimize) {
      HTMLOptimizer.optimize(destinationDirectory, destinationDirectory, fileName, "/js/optimized.js")
    }

    // TODO: support CSS merging
    // TODO: support HTML minification

    synchronized {
      if (removeDotHTML) {
        pages += (fileName.substring(0, fileName.indexOf('.')))
      } else {
        pages += fileName
      }
    }
  }

  def compilePartial(filePath: String): String = {
    val source = new File(sourceDirectory, s"partials/$filePath")
    compileHTML(source)
  }

  def compileHTML(source: File): String = {
    val stream = HTMLParser.cache(source)
    val deltas = List(
      // Include support
      Delta.Process(ByTag("include"), replace = true, onlyOpenTag = true, (openTag, content) => {
        val src = openTag.attributes("src")
        if (openTag.close.nonEmpty) {
          val html = compilePartial(src)
          val contentIndex = html.indexOf("<content/>")
          assert(contentIndex != -1, s"Any include must contain a <content/> block, but one was not found in $src")
          html.substring(0, contentIndex)
        } else {
          compilePartial(src)
        }
      }, Some((openTag, closeTag, content) => {
        val src = openTag.attributes("src")
        val html = compilePartial(src)
        html.substring(html.indexOf("<content/>") + 10)
      }))
    )
    stream.stream(deltas)
  }

  def copyAssets(): Unit = {
    val assets = new File(sourceDirectory, "assets")
    if (assets.exists()) {
      IO.copy(assets, destinationDirectory)
    }
  }

  def deleteDestination(): Unit = {
    IO.delete(destinationDirectory)
  }

  def compileAllLess(): Unit = {
    val lessDirectory = new File(sourceDirectory, "less")
    if (lessDirectory.exists()) {
      lessDirectory.listFiles().foreach {
        case f if f.isFile && f.getName.endsWith(".less") => {
          compileLess(f.getName, compressCSS)
        }
        case _ => // Ignore others
      }
    }
  }

  def compileLess(filePath: String, compress: Boolean): Unit = {
    val input = new File(sourceDirectory, s"less/$filePath")
    val output = new File(destinationDirectory, s"css/${input.getName.substring(0, input.getName.lastIndexOf('.'))}.css")
    scribe.info(s"Compiling LESS file ${input.getName}...")
    val command = new File(sourceDirectory.getParentFile, "node_modules/less/bin/lessc").getAbsolutePath
    val b = ListBuffer.empty[String]
    b += command
    if (compress) {
      b += "--compress"
    }
    b += input.getAbsolutePath
    b += output.getAbsolutePath
    val exitCode = b ! LoggingProcessLogger
    if (exitCode != 0) {
      throw new RuntimeException(s"Failed to compile LESS code!")
    }
  }

  def compileAllSass(): Unit = {
    val sassDirectory = new File(sourceDirectory, "sass")
    if (sassDirectory.exists()) {
      sassDirectory.listFiles().foreach {
        case f if f.isFile && (f.getName.endsWith(".sass") || f.getName.endsWith(".scss")) && !f.getName.startsWith("_") => {
          compileSass(f.getName, compressCSS)
        }
        case _ => // Ignore others
      }
    }
  }

  private lazy val sassCompiler = new io.bit3.jsass.Compiler

  def compileSass(filePath: String, compress: Boolean): Unit = {
    val input = new File(sourceDirectory, s"sass/$filePath")
    val output = new File(destinationDirectory, s"css/${input.getName.substring(0, input.getName.lastIndexOf('.'))}.css")
    val mapOutput = new File(destinationDirectory, s"css/${input.getName.substring(0, input.getName.lastIndexOf('.'))}.css.map")

    scribe.info(s"Compiling SASS file ${input.getName}...")

    output.getParentFile.mkdirs()
    val sass = IO.stream(input, new StringBuilder).toString
    val options = new io.bit3.jsass.Options
    if (compress) {
      options.setOutputStyle(OutputStyle.COMPRESSED)
      options.setSourceMapFile(new URI(mapOutput.getName))
    }
    val result = sassCompiler.compileString(sass, new URI(input.getName), new URI(output.getName), options)
    val css = result.getCss
    IO.stream(css, output)

    if (compress) {
      val map = result.getSourceMap
      IO.stream(map, mapOutput)
    }
  }
}