package io.youi.template

import com.outr.hookup.{Hookup, HookupSupport}

import scribe.Execution.global

trait TemplateHookup extends Hookup {
  val communication: TemplateCommunication with HookupSupport = auto[TemplateCommunication]
}
