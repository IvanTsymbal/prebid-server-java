package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BrandBlockingConfig;
import org.prebid.server.vertx.httpclient.HttpClient;
import org.prebid.server.vertx.httpclient.model.HttpClientResponse;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BrandBlockingStateCheckerTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private HttpClient httpClient;

    @Mock
    private BrandBlockingConfig brandBlockingConfig;

    @Mock
    private BidsScanResultProcessor bidsScanResultProcessor;

    private BrandBlockingStateChecker target;

    @Before
    public void setUp() {
        target = new BrandBlockingStateChecker(brandBlockingConfig, bidsScanResultProcessor, Vertx.vertx(), httpClient, new ObjectMapper());
    }

    @Test
    public void runShouldDoNothingWhenConfigUrlIsNotConfigured() {
        // given
        final String configUrl = "";
        doReturn(configUrl).when(brandBlockingConfig).getConfigUrl();

        // when
        target.run();

        // then
        verify(httpClient, times(0)).get(configUrl, 1000L);
    }

    @Test
    public void runShouldLoadInitialConfigsAndReloadThemWhenConfigUrlIsPresent() throws InterruptedException {
        // given
        final String configUrl = "https://server.com";
        final int stateCheckInterval = 1000;
        final long timeout = 1000L;

        doReturn(configUrl).when(brandBlockingConfig).getConfigUrl();
        doReturn(stateCheckInterval).when(brandBlockingConfig).getStateCheckInterval();
        doReturn(timeout).when(brandBlockingConfig).getTimeout();

        givenHttpClientReturnsResponse(httpClient, 200, "[]");

        // when
        target.run();

        // then
        Thread.sleep(timeout + 100);
        verify(httpClient, times(2)).get(configUrl, timeout);
    }

    @Test
    public void runShouldHandleErrorsWhenConfigRequestIsFailed() {
        // given
        final String configUrl = "https://server.com";
        final int stateCheckInterval = 1000;
        final long timeout = 1000L;

        doReturn(configUrl).when(brandBlockingConfig).getConfigUrl();
        doReturn(stateCheckInterval).when(brandBlockingConfig).getStateCheckInterval();
        doReturn(timeout).when(brandBlockingConfig).getTimeout();

        givenHttpClientReturnsResponse(httpClient, 404, "[]");

        // when
        target.run();

        // then
        verify(httpClient).get(configUrl, timeout);
    }

    private static void givenHttpClientReturnsResponse(HttpClient httpClient, int statusCode, String response) {
        final HttpClientResponse httpClientResponse = HttpClientResponse.of(statusCode, null, response);
        given(httpClient.get(anyString(), anyLong()))
                .willReturn(Future.succeededFuture(httpClientResponse));
    }
}
