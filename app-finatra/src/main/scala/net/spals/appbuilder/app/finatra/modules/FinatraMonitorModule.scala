package net.spals.appbuilder.app.finatra.modules

import java.util.concurrent.atomic.AtomicReference

import com.google.inject.matcher.Matchers.subclassesOf
import com.google.inject.spi.ProvisionListener
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule
import io.opentracing.Tracer
import net.spals.appbuilder.app.finatra.monitor.FinatraTracingFilter
import net.spals.appbuilder.config.matcher.BindingMatchers.keyTypeThat
import net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat

/**
  * @author tkral
  */
private[finatra] class FinatraMonitorModule
  extends TwitterModule
  with ProvisionListener {

  private val tracerRef = new AtomicReference[Tracer]()

  override def configure(): Unit = {
    val bindingMatcher = keyTypeThat(rawTypeThat(subclassesOf(classOf[Tracer])))
    bindListener(bindingMatcher, this)
  }

  override def onProvision[T](provision: ProvisionListener.ProvisionInvocation[T]): Unit = {
    val monitorComponent = provision.provision()

    monitorComponent match {
      case tracer: Tracer => tracerRef.set(tracer)
      case _ => warn(s"Encountered unknown monitoring component: ${monitorComponent.getClass}")
    }
  }

  private[finatra] def runMonitoringAutoBind(router: HttpRouter) = {
    Option(tracerRef.get()).map(new FinatraTracingFilter(_))
      .foreach(router.filter(_, beforeRouting = false))
  }
}
