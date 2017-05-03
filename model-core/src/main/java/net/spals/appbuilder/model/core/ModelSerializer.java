package net.spals.appbuilder.model.core;

import net.spals.appbuilder.annotations.service.AutoBindInMap;

import java.io.IOException;

/**
 * @author tkral
 */
public interface ModelSerializer {

    Object deserialize(final byte[] serializedModelObject) throws IOException;

    byte[] serialize(final Object modelObject) throws IOException;
}
