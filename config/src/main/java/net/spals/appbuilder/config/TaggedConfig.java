package net.spals.appbuilder.config;

/**
 * @author tkral
 */
public interface TaggedConfig {

    String ACTIVE_KEY = "active";
    String TAG_KEY = "tag";

    boolean isActive();

    String getTag();
}
