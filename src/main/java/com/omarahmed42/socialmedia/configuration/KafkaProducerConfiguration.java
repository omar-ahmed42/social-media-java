package com.omarahmed42.socialmedia.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.DelegatingSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.omarahmed42.socialmedia.dto.event.PublishedMessage;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.FriendRequest;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.cache.Newsfeed;

@Configuration
public class KafkaProducerConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    private static final String DELIMITER = ":";

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        configProps.put(DelegatingSerializer.VALUE_SERIALIZATION_SELECTOR_CONFIG, getTypeMappings(Newsfeed.class, Comment.class, Post.class, FriendRequest.class, Long.class,
                        String.class, Map.class, HashMap.class, Object.class));

        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.TYPE_MAPPINGS,
                getTypeMappings(PublishedMessage.class, Newsfeed.class, Comment.class, Post.class, FriendRequest.class, Long.class,
                        String.class, Map.class, HashMap.class, Object.class));
                        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    private String getTypeMappings(Class<?>... clazzes) {
        StringBuilder typesMappings = new StringBuilder();
        for (int i = 0; i < clazzes.length; i++) {
            Class<?> clazz = clazzes[i];
            String typeMapping = clazz.getSimpleName().toLowerCase() + DELIMITER + clazz.getCanonicalName();
            // System.out.println("TYPE: " + typeMapping);
            typesMappings.append(typeMapping);
            if (i + 1 < clazzes.length)
                typesMappings.append(", ");
        }
        return typesMappings.toString();
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
