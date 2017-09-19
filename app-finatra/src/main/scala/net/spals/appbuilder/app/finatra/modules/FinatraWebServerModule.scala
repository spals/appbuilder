package net.spals.appbuilder.app.finatra.modules

import com.google.inject.TypeLiteral
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.twitter.finagle.{Filter, http => finaglehttp}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule
import net.spals.appbuilder.app.core.matcher.TypeLiteralMatchers
import net.spals.appbuilder.graph.model.ServiceGraph

import scala.collection.mutable.ListBuffer

/**
  * @author tkral
  */
private[finatra] case class FinatraWebServerModule(
    serviceGraph: ServiceGraph,
    runWebServerAutoBinding: Boolean = true
  ) extends TwitterModule
  with InjectionListener[AnyRef]
  with TypeListener {

  private lazy val wsComponents = new ListBuffer[AnyRef]

  override def afterInjection(wsComponent: AnyRef): Unit = {
    wsComponents += wsComponent
  }

  override def configure(): Unit = {
    val typeMatcher = TypeLiteralMatchers.subclassesOf(classOf[Controller])
        .or(TypeLiteralMatchers.subclassesOf(classOf[ExceptionMapper[_]]))
        .or(TypeLiteralMatchers.subclassesOf(classOf[Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response]]))

    bindListener(typeMatcher, this)
  }

  override def hear[I](typeLiteral: TypeLiteral[I], typeEncounter: TypeEncounter[I]): Unit = {
    typeEncounter.register(this.asInstanceOf[InjectionListener[I]])
  }

  private[finatra] def runWebServerAutoBind(router: HttpRouter): Unit = {
    val controllers = new ListBuffer[Controller]
    val exceptionMappers = new ListBuffer[ExceptionMapper[_ <: Throwable]]
    val filters = new ListBuffer[Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response]]
    Option(runWebServerAutoBinding).filter(b => b).foreach(b => {
      val wsComponentsList = wsComponents.toList
      wsComponentsList.foreach(
        wsComponent => {
          wsComponent match {
            case controller: Controller => controllers += controller
            case mapper: AnyRef if mapper.isInstanceOf[ExceptionMapper[_]] =>
              exceptionMappers += mapper.asInstanceOf[ExceptionMapper[_ <: Throwable]]
            case filter: AnyRef if filter.isInstanceOf[Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response]] =>
              filters += filter.asInstanceOf[Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response]]
          }
        }
    )})

    filters.toList.foreach(filter => {
      info(s"Binding filter: ${filter.getClass}")
      router.filter(filter)
    })

    exceptionMappers.toList.foreach(exceptionMapper => {
      info(s"Binding exception mapper: ${exceptionMapper.getClass}")
      router.exceptionMapper(exceptionMapper)
    })

    controllers.toList.foreach(controller => {
      info(s"Binding controller: ${controller.getClass}")
      router.add(controller)
    })
  }
}
