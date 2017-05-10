package net.spals.appbuilder.message.core.producer;

/**
 * @author tkral
 */
public interface MessageProducer {

    void sendMessage(String tag, Object payload);

}
