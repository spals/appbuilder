package net.spals.appbuilder.app.finatra.sample

import com.google.inject.Inject
import com.google.inject.name.Named
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * An auto-bound service for [[SampleFinatraWebApp]]
  *
  * @author tkral
  */
@AutoBindSingleton
private[finatra] class SampleFinatraCustomService @Inject() (@Named("AutoBoundModule") autoBoundModuleName: String) { }
