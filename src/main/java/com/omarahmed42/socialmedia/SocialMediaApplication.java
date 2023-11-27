package com.omarahmed42.socialmedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.cassandra.config.EnableCassandraAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;

@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableCassandraAuditing
@EnableNeo4jAuditing
public class SocialMediaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SocialMediaApplication.class, args);
	}

}
