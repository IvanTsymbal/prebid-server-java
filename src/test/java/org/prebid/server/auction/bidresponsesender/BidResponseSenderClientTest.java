package org.prebid.server.auction.bidresponsesender;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BidResponseSenderClientTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private RestTemplate restTemplate;

    private BidResponseSenderClient bidResponseSenderClient;

    @Test
    public void shouldNotThrowAnErrorWhenClientUrlIsMissing() {
        // given
        bidResponseSenderClient = new BidResponseSenderClient("", restTemplate);

        // when
        bidResponseSenderClient.sendDataToServer("");

        // then
        verify(restTemplate, times(0)).postForEntity(any(), any(), any());
    }

    @Test
    public void shouldSendHTTPRequest() throws JSONException {
        // given
        final String url = "https://www.client.com/stats/v1";
        final JSONObject dataToSend = new JSONObject();
        dataToSend.put("key", "value");
        bidResponseSenderClient = new BidResponseSenderClient(url, restTemplate);
        when(restTemplate.postForEntity(eq(url), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.status(200).body(""));

        // when
        bidResponseSenderClient.sendDataToServer(dataToSend);

        // then
        verify(restTemplate, times(1)).postForEntity(eq(url), any(), eq(Object.class));
    }
}
