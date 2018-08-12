package net.spals.appbuilder.model.core;

import com.google.inject.Inject;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link ModelSerializerRegistry}.
 *
 * @author tkral
 */
@AutoBindSingleton
class DefaultModelSerializerRegistry implements ModelSerializerRegistry {

    private final Map<String, ModelSerializer> serializerMap;

    @Inject
    DefaultModelSerializerRegistry(final Map<String, ModelSerializer> serializerMap) {
        this.serializerMap = serializerMap;
    }


    @Override
    public Set<String> getAvailableModelSerializers() {
        return serializerMap.keySet();
    }

    @Override
    public ModelSerializer getModelSerializer(final String key) {
        return serializerMap.get(key);
    }
}
