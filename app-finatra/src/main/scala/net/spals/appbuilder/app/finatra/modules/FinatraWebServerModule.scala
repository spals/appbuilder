package net.spals.appbuilder.app.finatra.modules

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

import scala.collection.mutable.ListBuffer

/**
  * @author tkral
  */
private[finatra] case class FinatraWebServerModule(
    serviceGraph: ServiceGraph,
    addCommonFilters: Boolean = true,
    addRequestScopeFilter: Boolean = false,
    runWebServerAutoBinding: Boolean = true
  ) extends TwitterModule
  with InjectionListener[AnyRef]
  with TypeListener {

  private lazy val wsComponents = new ListBuffer[AnyRef]

  override def afterInjection(wsComponent: AnyRef): Unit = {
//    wsComponents += wsComponent
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
    Option(addCommonFilters).filter(b => b).foreach(router.filter[CommonFilters])
    Option(addRequestScopeFilter).filter(b => b)
      .foreach(router.filter[FinagleRequestScopeFilter[finaglehttp.Request, finaglehttp.Response]])

    Option(runWebServerAutoBinding).filter(b => b).foreach(b => wsComponents.foreach(
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
}
