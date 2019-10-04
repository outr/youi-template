package io.youi.template

case class TemplateConfig(mode: String = "compile",
                          modification: String = "none",
                          source: Option[String],
                          destination: Option[String],
                          removeDotHTML: Boolean = false,
                          spa: Boolean = true)
