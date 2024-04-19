package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.prebid.server.exception.PreBidException;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BrandBlockingConfig;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BrandBlockingData;
import org.prebid.server.vertx.httpclient.HttpClient;
import org.prebid.server.vertx.httpclient.model.HttpClientResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class BrandBlockingStateChecker {

    private static final Logger logger = LoggerFactory.getLogger(BrandBlockingStateChecker.class);

    private final BrandBlockingConfig brandBlockingConfig;

    private final BidsScanResultProcessor bidsScanResultProcessor;

    private final Vertx vertx;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public BrandBlockingStateChecker(
            BrandBlockingConfig brandBlockingConfig,
            BidsScanResultProcessor bidsScanResultProcessor,
            Vertx vertx,
            HttpClient httpClient,
            ObjectMapper objectMapper) {

        this.brandBlockingConfig = brandBlockingConfig;
        this.bidsScanResultProcessor = bidsScanResultProcessor;
        this.vertx = vertx;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void run() {
        if (!brandBlockingConfig.getConfigUrl().isEmpty()) {
            verifyConfigs();
            vertx.setPeriodic(brandBlockingConfig.getStateCheckInterval(), ignored -> verifyConfigs());
        }
    }

    private void verifyConfigs() {
        httpClient.get(brandBlockingConfig.getConfigUrl(), brandBlockingConfig.getTimeout())
                .map(this::parseConfigResponse)
                .map(this::updateBrandBlocking)
                .otherwise(this::handleErrorResponse);
    }

    private BrandBlockingData[] parseConfigResponse(HttpClientResponse httpClientResponse) {

        final int statusCode = httpClientResponse.getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new PreBidException("Failed to request Brand Blocking configs, response status: %s".formatted(statusCode));
        }
        final String body = httpClientResponse.getBody();
        try {
            return objectMapper.readValue(body, BrandBlockingData[].class);
        } catch (IOException e) {
            throw new PreBidException("Failed to parse Brand Blocking configs, cause: %s".formatted(ExceptionUtils.getMessage(e)));
        }
    }

    private Void updateBrandBlocking(BrandBlockingData[] brandBlockingData) {
        logger.info("Confiant Brand Blocking configs were updated");

        bidsScanResultProcessor.setBrandBlockingData(List.of(brandBlockingData));

        return null;
    }

    private Void handleErrorResponse(Throwable exception) {
        logger.warn("Error occurred while request Brand Blocking configs: %s".formatted(exception.getMessage()));

        bidsScanResultProcessor.setBrandBlockingData(Collections.emptyList());

        return null;
    }
}
