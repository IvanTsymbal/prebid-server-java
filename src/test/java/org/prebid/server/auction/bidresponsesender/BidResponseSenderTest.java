package org.prebid.server.auction.bidresponsesender;

import com.iab.openrtb.request.BidRequest;
import com.iab.openrtb.response.BidResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.prebid.server.model.HttpRequestContext;
import org.prebid.server.settings.model.Account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BidResponseSenderTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BidResponseSenderClient bidResponseSenderClient;

    private BidResponseSender bidResponseSender;

    private static final String ACCOUNT_ID = "acc_id";

    private Account defaultAccount;
    private BidRequest defaultBidRequest;
    private BidResponse defaultBidResponse;
    private HttpRequestContext defaultHttpRequestContext;

    @Before
    public void setUp() {
        bidResponseSender = new BidResponseSender(bidResponseSenderClient);
        defaultBidRequest = BidRequest.builder().build();
        defaultBidResponse = BidResponse.builder().build();
        defaultAccount = Account.empty(ACCOUNT_ID);
        defaultHttpRequestContext = HttpRequestContext.builder().build();
    }

    @Test
    public void shouldSendDataToExternalClientOnBidPostProcess() {
        // given

        // when
        bidResponseSender.postProcess(
                defaultHttpRequestContext,
                null,
                defaultBidRequest,
                defaultBidResponse,
                defaultAccount
        );

        // then
        verify(bidResponseSenderClient, times(1)).sendDataToServer(any());
    }
}
