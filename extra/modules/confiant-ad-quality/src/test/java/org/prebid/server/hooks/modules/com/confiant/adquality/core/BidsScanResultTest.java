package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BidsScanResultTest {

    private final RedisParser redisParser = new RedisParser(new ObjectMapper());

    @Test
    public void getIssuesMessagesShouldProperlyGenerateMessage() {
        // given
        final String redisResponse = "[[[{\"tag_key\": \"key_a\", \"imp_id\": \"imp_a\", \"issues\": [{ \"value\": \"ads.deceivenetworks.net\", \"spec_name\": \"malicious_domain\", \"first_adinstance\": \"e91e8da982bb8b7f80100426\"}]}]]]";
        final BidsScanResult bidsScanResult = redisParser.parseBidsScanResult(redisResponse);

        // when
        final List<String> issues = bidsScanResult.getIssuesMessages();

        // then
        assertThat(issues.size()).isEqualTo(1);
        assertThat(issues.get(0)).isEqualTo("key_a: [Issue(specName=malicious_domain, value=ads.deceivenetworks.net, firstAdinstance=e91e8da982bb8b7f80100426)]");
    }

    @Test
    public void getDebugMessagesShouldProperlyGenerateDebugMessage() {
        // given
        final String redisResponse = "invalid redis response";
        final BidsScanResult bidsScanResult = redisParser.parseBidsScanResult(redisResponse);

        // when
        final List<String> messages = bidsScanResult.getDebugMessages();

        // then
        assertThat(messages.size()).isEqualTo(1);
        assertThat(messages.get(0)).isEqualTo("Error during parse redis response: invalid redis response");
    }
}
