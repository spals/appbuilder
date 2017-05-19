package net.spals.appbuilder.app.finatra.modules

import com.google.inject.Key
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.FlagImpl
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * Guice module which binds Typesafe [[Config]] values
  * as injectable Finatra flags.
  *
  * @author tkral
  */
private[finatra] case class AutoBindConfigFlagsModule(config: Config) extends TwitterModule {

  override def configure() {
    for (configEntry <- config.entrySet().asScala) {
      val configValueStr = String.valueOf(configEntry.getValue.unwrapped())
      val key = Key.get(classOf[java.lang.String], new FlagImpl(configEntry.getKey))
      debug(s"Binding config flag: ${configEntry.getKey} = $configValueStr")
      binder.bind(key).toInstance(configValueStr)
    }
  }
}
