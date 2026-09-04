package com.sjeom.mydata.platform.support.config;

import com.sjeom.mydata.platform.ai.llm.LlmClient;
import com.sjeom.mydata.platform.ai.llm.OpenAiCompatibleLlmClient;
import com.sjeom.mydata.platform.ai.llm.PrivateLlmProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("poc & private-llm")
@EnableConfigurationProperties(PrivateLlmProperties.class)
public class PrivateLlmConfiguration {

    @Bean
    LlmClient privateLlmClient(
            RestClient.Builder restClientBuilder,
            PrivateLlmProperties properties
    ) {
        return new OpenAiCompatibleLlmClient(restClientBuilder, properties);
    }
}
