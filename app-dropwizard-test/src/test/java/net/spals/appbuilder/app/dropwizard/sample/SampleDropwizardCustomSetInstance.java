package net.spals.appbuilder.app.dropwizard.sample;

import net.spals.appbuilder.annotations.service.AutoBindInSet;

/**
 * An auto-bound multi service for {@link SampleDropwizardWebApp}
 *
 * @author tkral
 */
@AutoBindInSet(baseClass = SampleDropwizardCustomSet.class)
class SampleDropwizardCustomSetInstance implements SampleDropwizardCustomSet {
}
