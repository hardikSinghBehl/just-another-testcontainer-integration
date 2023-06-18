package com.behl.receptacle;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

public class KafkaTestUtil {

    private KafkaTestUtil() {
    }

    public static Consumer<Object, Object> getConsumer(final String bootstrapServers, final String topicName) {
        final var properties = new HashMap<String, Object>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);   
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());    
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.toString());
        final var consumer = new DefaultKafkaConsumerFactory<>(properties).createConsumer();
        consumer.subscribe(List.of(topicName));
        return consumer;
    }

}