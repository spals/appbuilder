package net.spals.appbuilder.app.finatra.minimal

import net.spals.appbuilder.app.finatra.FinatraWebApp

/**
  * A minimally viable [[FinatraWebApp]]
  *
  * @author tkral
  */
object MinimalFinatraWebAppMain extends MinimalFinatraWebApp

private[finatra] class MinimalFinatraWebApp extends FinatraWebApp {

  build()

}
