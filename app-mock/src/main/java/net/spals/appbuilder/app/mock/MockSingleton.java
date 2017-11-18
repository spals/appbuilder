package net.spals.appbuilder.app.mock;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

/**
 * An interface for mock service singletons that are to be
 * added to a {@link MockApp}.
 * <p/>
 * This emulate the {@link AutoBindSingleton} annotation.
 * It allows a tester to add binding details to a hand
 * created a mock service. These details are the same as
 * what the {@link AutoBindSingleton} annotation provides
 * at production runtime.
 *
 * @author tkral
 */
public interface MockSingleton<I> {

    /**
     * @see {@link AutoBindSingleton#baseClass()}
     */
    Class<I> baseClass();
}
