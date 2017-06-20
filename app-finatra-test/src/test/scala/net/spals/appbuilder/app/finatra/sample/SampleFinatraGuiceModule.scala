package net.spals.appbuilder.app.finatra.sample

import com.google.inject.AbstractModule
import com.google.inject.name.Names

/**
  * A sample guice [[com.google.inject.Module]] for
  * [[net.spals.appbuilder.app.finatra.FinatraWebApp#addModule]]
  *
  * @author tkral
  */
private[sample] class SampleFinatraGuiceModule extends AbstractModule {
  override def configure(): Unit = {
    binder.bind(classOf[String]).annotatedWith(Names.named("GuiceModule")).toInstance(this.getClass.getName)
  }
}
