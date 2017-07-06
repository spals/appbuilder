package net.spals.appbuilder.app.finatra.sample

import com.google.inject.Inject
import com.google.inject.name.Names
import com.twitter.inject.TwitterModule
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindModule

/**
  * A sample [[TwitterModule]] for testing module auto-binding.
  *
  * @author tkral
  */
@AutoBindModule
private[sample] class SampleFinatraAutoBoundModule @Inject()(@ApplicationName applicationName: String)
  extends TwitterModule {

  override def configure(): Unit = {
    binder.bind(classOf[String]).annotatedWith(Names.named("AutoBoundModule"))
      .toInstance(s"$applicationName:${this.getClass.getName}")
  }
}
