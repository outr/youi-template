package io.youi.template

import com.outr.hookup.client

import scala.concurrent.Future

trait TemplateCommunication {
  @client def reload(force: Boolean): Future[Unit]
}
