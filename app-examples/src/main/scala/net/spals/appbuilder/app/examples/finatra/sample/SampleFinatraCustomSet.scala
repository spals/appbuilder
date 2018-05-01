package net.spals.appbuilder.app.examples.finatra.sample

import net.spals.appbuilder.annotations.service.AutoBindInSet

/**
  * A multi service definition for [[SampleFinatraWebApp]]
  *
  * @author tkral
  */
trait SampleFinatraCustomSet {  }

/**
  * An auto-bound multi service for [[SampleFinatraWebApp]]
  *
  * @author tkral
  */
@AutoBindInSet(baseClass = classOf[SampleFinatraCustomSet])
private[finatra] class SampleFinatraCustomSetInstance extends SampleFinatraCustomSet {  }
