package net.spals.appbuilder.app.finatra.modules

import java.util.concurrent.atomic.AtomicBoolean

import com.google.inject.TypeLiteral
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.twitter.finagle.{Filter, http => finaglehttp}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions.{ExceptionMapper, ExceptionMapperCollection}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule
import com.twitter.inject.requestscope.FinagleRequestScopeFilter
import net.spals.appbuilder.app.core.matcher.TypeLiteralMatchers
import net.spals.appbuilder.graph.model.ServiceGraph

/**
  * @author tkral
  */
private[finatra] case class FinatraWebServerModule(serviceGraph: ServiceGraph) extends TwitterModule
  with InjectionListener[AnyRef]
  with TypeListener {

  private[finatra] val addCommonFilters = new AtomicBoolean(true)
  private[finatra] val addRequestScopeFilter = new AtomicBoolean(false)
  private[finatra] val runWebServerAutoBinding = new AtomicBoolean(true)

  override def afterInjection(wsComponent: AnyRef): Unit = {
    wsComponents ++ Seq(wsComponent)
  }

  override def configure(): Unit = {
    val typeMatcher = TypeLiteralMatchers.subclassesOf(classOf[Controller])
        .or(TypeLiteralMatchers.subclassesOf(classOf[ExceptionMapper[_]]))
        .or(TypeLiteralMatchers.subclassesOf(classOf[ExceptionMapperCollection]))
        .or(TypeLiteralMatchers.subclassesOf(classOf[Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response]]))

    bindListener(typeMatcher, this)
  }

  override def hear[I](typeLiteral: TypeLiteral[I], typeEncounter: TypeEncounter[I]): Unit = {
    typeEncounter.register(this.asInstanceOf[InjectionListener[I]])
  }

  private[finatra] def runWebServerAutoBind(router: HttpRouter): Unit = {
    Option(addCommonFilters.get).filter(b => b).foreach(router.filter[CommonFilters])
    Option(addRequestScopeFilter.get).filter(b => b)
      .foreach(router.filter[FinagleRequestScopeFilter[finaglehttp.Request, finaglehttp.Response]])

    Option(runWebServerAutoBinding.get).filter(b => b).foreach(b => wsComponents.foreach(
      wsComponent => {
        wsComponent match {
          case controller: Controller => router.add(controller)
//          case mapper: ExceptionMapper[_] => router.exceptionMapper(mapper)
          case mappers: ExceptionMapperCollection => router.exceptionMapper(mappers)
//          case filter: Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response] =>
//            router.filter(filter)
        }
      }
    ))
  }

  private def wsComponents: Seq[AnyRef] = Seq()
}
