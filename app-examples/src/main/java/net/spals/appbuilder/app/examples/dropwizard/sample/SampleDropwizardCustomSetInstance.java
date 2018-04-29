package net.spals.appbuilder.app.examples.dropwizard.sample;

import net.spals.appbuilder.annotations.service.AutoBindInSet;

/**
 * An auto-bound multi service for {@link SampleDropwizardWebApp}
 *
 * @author tkral
 */
@AutoBindInSet(baseClass = SampleDropwizardCustomSet.class)
class SampleDropwizardCustomSetInstance implements SampleDropwizardCustomSet {
}
