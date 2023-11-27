package com.omarahmed42.socialmedia.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.omarahmed42.socialmedia.dto.event.FriendRequestEvent;
import com.omarahmed42.socialmedia.dto.event.PublishedMessage;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.FriendRequest;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.cache.Newsfeed;

@EnableKafka
@Configuration
public class KafkaConsumerConfiguration {

        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        @Value("${spring.kafka.consumer.group-id}")
        private String groupId;

        private static final String DELIMITER = ":";

        @Bean
        public ConsumerFactory<String, Object> consumerFactory() {
                Map<String, Object> props = new HashMap<>();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
                props.put(JsonDeserializer.TYPE_MAPPINGS,
                                getTypeMappings(PublishedMessage.class, Newsfeed.class, Comment.class, Post.class, FriendRequest.class,
                                                FriendRequestEvent.class,
                                                Long.class, String.class, Map.class, HashMap.class, Object.class));
                return new DefaultKafkaConsumerFactory<>(props);
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
        public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {

                ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(consumerFactory());
                return factory;
        }

}
