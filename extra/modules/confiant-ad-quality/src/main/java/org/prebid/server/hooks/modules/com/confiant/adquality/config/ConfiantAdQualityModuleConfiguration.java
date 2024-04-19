package org.prebid.server.hooks.modules.com.confiant.adquality.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.prebid.server.auction.privacy.enforcement.mask.UserFpdActivityMask;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.BidsScanResultProcessor;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.BidsScanner;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.BrandBlockingStateChecker;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.RedisClient;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.RedisScanStateChecker;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BrandBlockingConfig;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.RedisConfig;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.RedisConnectionConfig;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.RedisRetryConfig;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.ConfiantAdQualityBidResponsesScanHook;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.ConfiantAdQualityModule;
import org.prebid.server.spring.env.YamlPropertySourceFactory;
import org.prebid.server.vertx.httpclient.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(prefix = "hooks." + ConfiantAdQualityModule.CODE, name = "enabled", havingValue = "true")
@PropertySource(
        value = "classpath:/module-config/confiant-ad-quality.yaml",
        factory = YamlPropertySourceFactory.class)
@Configuration
public class ConfiantAdQualityModuleConfiguration {

    private final static String READ_REDIS_NODE = "read";
    private final static String WRITE_REDIS_NODE = "write";

    @Bean
    ConfiantAdQualityModule confiantAdQualityModule(
            @Value("${hooks.modules.confiant-ad-quality.api-key}") String apiKey,
            @Value("${hooks.modules.confiant-ad-quality.scan-state-check-interval}") int scanStateCheckInterval,
            @Value("${hooks.modules.confiant-ad-quality.bidders-to-exclude-from-scan}") List<String> biddersToExcludeFromScan,
            RedisConfig redisConfig,
            RedisRetryConfig retryConfig,
            BrandBlockingConfig brandBlockingConfig,
            Vertx vertx,
            HttpClient httpClient,
            UserFpdActivityMask userFpdActivityMask,
            ObjectMapper objectMapper) {

        final Map<String, RedisClient> redisClientsHashMap = getRedisClients(redisConfig, retryConfig, vertx);

        final BidsScanner bidsScanner = new BidsScanner(
                redisClientsHashMap.get(WRITE_REDIS_NODE),
                redisClientsHashMap.get(READ_REDIS_NODE),
                apiKey,
                objectMapper);

        final BidsScanResultProcessor bidsScanResultProcessor = new BidsScanResultProcessor();
        final BrandBlockingStateChecker brandBlockingStateChecker = new BrandBlockingStateChecker(
                brandBlockingConfig,
                bidsScanResultProcessor,
                vertx,
                httpClient,
                objectMapper);

        final RedisScanStateChecker redisScanStateChecker = new RedisScanStateChecker(
                bidsScanner,
                scanStateCheckInterval,
                vertx);

        final Promise<Void> scannerPromise = Promise.promise();
        scannerPromise.future().onComplete(r -> {
            redisScanStateChecker.run();
            brandBlockingStateChecker.run();
        });

        bidsScanner.start(scannerPromise);

        return new ConfiantAdQualityModule(Collections.singletonList(
                new ConfiantAdQualityBidResponsesScanHook(
                        bidsScanner, bidsScanResultProcessor, biddersToExcludeFromScan, userFpdActivityMask)));
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConfigurationProperties(prefix = "hooks.modules.confiant-ad-quality.redis-config")
    RedisConfig redisConfig() {
        return new RedisConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "hooks.modules.confiant-ad-quality.redis-retry-config")
    RedisRetryConfig redisRetryConfig() {
        return new RedisRetryConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "hooks.modules.confiant-ad-quality.brand-blocking-config")
    BrandBlockingConfig brandBlockingConfig() {
        return new BrandBlockingConfig();
    }

    private Map<String, RedisClient> getRedisClients(
            RedisConfig redisConfig,
            RedisRetryConfig retryConfig,
            Vertx vertx) {

        final RedisConnectionConfig writeNodeConfig = redisConfig.getWriteNode();
        final RedisClient writeRedisNode = new RedisClient(
                vertx, writeNodeConfig.getHost(), writeNodeConfig.getPort(), writeNodeConfig.getPassword(), retryConfig, WRITE_REDIS_NODE);
        final RedisConnectionConfig readNodeConfig = redisConfig.getReadNode();
        final RedisClient readRedisNode = new RedisClient(
                vertx, readNodeConfig.getHost(), readNodeConfig.getPort(), readNodeConfig.getPassword(), retryConfig, READ_REDIS_NODE);

        final Map<String, RedisClient> redisClientsHashMap = new HashMap<>();
        redisClientsHashMap.put(WRITE_REDIS_NODE, writeRedisNode);
        redisClientsHashMap.put(READ_REDIS_NODE, readRedisNode);

        return redisClientsHashMap;
    }

    private void initBidsScanner() {

    }
}
