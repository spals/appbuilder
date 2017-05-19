package net.spals.appbuilder.message.core;

/**
 * @author tkral
 */
public interface MessageProducer {

    void sendMessage(String tag, Object payload);

}
