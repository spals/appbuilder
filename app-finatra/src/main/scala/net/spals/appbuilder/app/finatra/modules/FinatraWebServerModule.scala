package net.spals.appbuilder.app.finatra.modules

import java.util.Optional

import com.google.inject.matcher.Matchers.subclassesOf
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.google.inject.{Key, TypeLiteral}
import com.twitter.finagle.{Filter, http => finaglehttp}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule
import net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat
import net.spals.appbuilder.graph.model.{ServiceDAG, ServiceDAGVertex}

import scala.collection.mutable.ListBuffer

/**
  * @author tkral
  */
private[finatra] case class FinatraWebServerModule(
    serviceDAG: ServiceDAG,
    runWebServerAutoBinding: Boolean = true
  ) extends TwitterModule
  with InjectionListener[AnyRef]
  with TypeListener {

  private lazy val webServerVertex = new FinatraWebServerVertex
  private lazy val wsComponents = new ListBuffer[AnyRef]

  override def afterInjection(wsComponent: AnyRef): Unit = {
    wsComponents += wsComponent
  }

  override def configure(): Unit = {
    val typeMatcher = rawTypeThat(subclassesOf(classOf[Controller]))
        .or(rawTypeThat(subclassesOf(classOf[ExceptionMapper[_]])))
        .or(rawTypeThat(subclassesOf(classOf[Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response]])))

    bindListener(typeMatcher, this)
  }

  override def hear[I](typeLiteral: TypeLiteral[I], typeEncounter: TypeEncounter[I]): Unit = {
    typeEncounter.register(this.asInstanceOf[InjectionListener[I]])
  }

  private[finatra] def addComponentVertex[T](wsComponent: T): Unit = {
    val wsKey: Key[T] = Key.get(TypeLiteral.get(wsComponent.getClass.asInstanceOf[Class[T]]))
    val wsVertex = ServiceDAGVertex.createVertex(wsKey, wsComponent)
    serviceDAG.addVertex(wsVertex)
    serviceDAG.addEdge(wsVertex, webServerVertex)
  }

  private[finatra] def runWebServerAutoBind(router: HttpRouter): Unit = {
    val controllers = new ListBuffer[Controller]
    val exceptionMappers = new ListBuffer[ExceptionMapper[_ <: Throwable]]
    val filters = new ListBuffer[Filter[finaglehttp.Request, finaglehttp.Response, finaglehttp.Request, finaglehttp.Response]]
    Option(runWebServerAutoBinding).filter(b => b).foreach(b => {
      // Add the dummy webserver vertex to the service DAG
      if(!serviceDAG.containsVertex(webServerVertex)) {
        serviceDAG.addVertex(webServerVertex)
      }

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
      addComponentVertex(filter)
    })

    exceptionMappers.toList.foreach(exceptionMapper => {
      info(s"Binding exception mapper: ${exceptionMapper.getClass}")
      router.exceptionMapper(exceptionMapper)
      addComponentVertex(exceptionMapper)
    })

    controllers.toList.foreach(controller => {
      info(s"Binding controller: ${controller.getClass}")
      router.add(controller)
      addComponentVertex(controller)
    })
  }
}

/**
  * Special {@link ServiceDAGVertex} instance which
  * represents a Finatra web server.
  *
  * All auto-bound webserver components will have an outgoing
  * edge to this vertex in order to show a complete graph.
  *
  * @author tkral
  */
private[modules] class FinatraWebServerVertex extends ServiceDAGVertex[String] {

  override def getGuiceKey: Key[String] = Key.get(classOf[String])

  override def getServiceInstance = "FINATRA WEBSERVER"

  override def getProviderSource: Optional[ServiceDAGVertex[_]] = Optional.empty[ServiceDAGVertex[_]]

  override protected def toString(separator: String): String = getServiceInstance
}
