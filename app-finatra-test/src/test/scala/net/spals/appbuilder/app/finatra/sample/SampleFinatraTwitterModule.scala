package net.spals.appbuilder.app.finatra.sample

import com.google.inject.name.Names
import com.twitter.inject.TwitterModule

/**
  * A sample [[TwitterModule]] for
  * [[net.spals.appbuilder.app.finatra.FinatraWebApp#addModule]]
  *
  * @author tkral
  */
private[sample] class SampleFinatraTwitterModule extends TwitterModule {
  override protected def configure(): Unit = {
    binder.bind(classOf[String]).annotatedWith(Names.named("TwitterModule")).toInstance(this.getClass.getName)
  }
}
