package org.prebid.server.auction.bidresponsesender;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BidResponseSenderClient {

    private static final Logger logger = LoggerFactory.getLogger(BidResponseSender.class);

    private final String clientUrl;

    private final RestTemplate restTemplate;

    public BidResponseSenderClient(
            @Value("auction.bid-response-sender-client-url") String clientUrl,
            RestTemplate restTemplate
    ) {
        this.clientUrl = clientUrl;
        this.restTemplate = restTemplate;
    }

    public <T> Void sendDataToServer(T data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String serverUrl = clientUrl;
        logger.info("1 {0}", serverUrl);

        if (serverUrl.isEmpty()) {
            logger.info("Bid response sender client URL is not configured");
            return null;
        }

        HttpEntity<T> requestEntity = new HttpEntity<T>(data, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(serverUrl, requestEntity, Object.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.info("Sending bid info was failed", response.getBody());
        }

        return null;
    }
}
