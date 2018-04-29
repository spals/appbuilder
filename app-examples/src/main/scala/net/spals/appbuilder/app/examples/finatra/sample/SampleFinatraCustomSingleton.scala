package net.spals.appbuilder.app.examples.finatra.sample

import com.google.common.annotations.VisibleForTesting
import com.google.inject.Inject
import com.google.inject.name.Named
import net.spals.appbuilder.annotations.service.AutoBindSingleton

/**
  * An auto-bound singleton service for [[SampleFinatraWebApp]]
  *
  * @author tkral
  */
@AutoBindSingleton
@VisibleForTesting
/*private[finatra]*/ class SampleFinatraCustomSingleton @Inject()(@Named("AutoBoundModule") autoBoundModuleName: String) { }
