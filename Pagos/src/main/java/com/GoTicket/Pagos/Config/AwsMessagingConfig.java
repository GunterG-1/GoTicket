package com.GoTicket.Pagos.Config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsMessagingConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    Region awsRegion(@Value("${spring.aws.region:us-east-1}") String region) { return Region.of(region); }

    @Bean(destroyMethod = "close")
    SnsClient snsClient(Region region, @Value("${spring.aws.endpoint:}") String endpoint) {
        var builder = SnsClient.builder().region(region);
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    SqsClient sqsClient(Region region, @Value("${spring.aws.endpoint:}") String endpoint) {
        var builder = SqsClient.builder().region(region);
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }
}
