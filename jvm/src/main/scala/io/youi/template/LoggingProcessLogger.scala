package io.youi.template

import scala.sys.process.ProcessLogger

object LoggingProcessLogger extends ProcessLogger {
  override def out(s: => String): Unit = System.out.println(s)

  override def err(s: => String): Unit = System.err.println(s)

  override def buffer[T](f: => T): T = f
}
