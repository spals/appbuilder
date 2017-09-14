package net.spals.appbuilder.monitor.lightstep

import java.util.Collections
import javax.validation.constraints.{NotNull, Pattern}

import com.google.inject.Inject
import com.lightstep.tracer.jre.JRETracer
import com.lightstep.tracer.shared.Options
import com.netflix.governator.annotations.Configuration
import io.opentracing.Tracer
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.monitor.core.{TracerPlugin, TracerTag}

import scala.collection.JavaConverters._

/**
  * A [[TracerPlugin]] for a Lightstep
  * Java [[Tracer]] instance.
  *
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[TracerPlugin], key = "lightstep")
private[lightstep] class LightstepTracerPlugin @Inject() (
  @ApplicationName applicationName: String
) extends TracerPlugin {

  @NotNull
  @Configuration("tracing.lightstep.accessToken")
  private[lightstep] var accessToken: String = null

  @Configuration("tracing.lightstep.collectorHost")
  private[lightstep] var collectorHost: String = null

  @Configuration("tracing.lightstep.collectorPort")
  private[lightstep] var collectorPort: Int = 0

  @Pattern(regexp = "(http|https)")
  @Configuration("tracing.lightstep.collectorProtocol")
  private[lightstep] var collectorProtocol: String = "http"

  @Configuration("tracing.lightstep.tracerTags")
  private[lightstep] var tracerTags: java.util.List[TracerTag] = Collections.emptyList[TracerTag]()

  override def createTracer(): Tracer = {
    val optionsBuilder = new Options.OptionsBuilder()
      .withAccessToken(accessToken)
      .withComponentName(applicationName)
      .withCollectorProtocol(collectorProtocol)

    Option(collectorHost).foreach(optionsBuilder.withCollectorHost(_))
    Option(collectorPort).filter(_ != 0).foreach(optionsBuilder.withCollectorPort(_))
    tracerTags.asScala.foreach(tracerTag => optionsBuilder.withTag(tracerTag.getKey, tracerTag.getValue))

    new JRETracer(optionsBuilder.build())
  }
}
