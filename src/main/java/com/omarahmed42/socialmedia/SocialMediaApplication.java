package com.omarahmed42.socialmedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.cassandra.config.EnableCassandraAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableJpaAuditing
@EnableCassandraAuditing(modifyOnCreate = false)
@EnableNeo4jAuditing
@EnableRetry
public class SocialMediaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SocialMediaApplication.class, args);
	}

}
