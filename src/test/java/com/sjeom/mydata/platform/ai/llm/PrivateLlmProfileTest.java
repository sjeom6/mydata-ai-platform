package com.sjeom.mydata.platform.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "platform.llm.base-url=http://private-llm.internal",
        "platform.llm.model=internal-model"
})
@ActiveProfiles({"poc", "private-llm"})
class PrivateLlmProfileTest {

    @Autowired
    private LlmClient llmClient;

    @Test
    void selectsPrivateAdapterInsteadOfPocMock() {
        assertThat(llmClient).isInstanceOf(OpenAiCompatibleLlmClient.class);
    }
}
