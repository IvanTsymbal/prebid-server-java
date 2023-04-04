package org.prebid.server.auction.bidresponsesender;

import com.iab.openrtb.request.BidRequest;
import com.iab.openrtb.response.BidResponse;
import io.vertx.core.Future;
import org.prebid.server.auction.BidResponsePostProcessor;
import org.prebid.server.cookie.UidsCookie;
import org.prebid.server.model.HttpRequestContext;
import org.prebid.server.settings.model.Account;

public class BidResponseSender implements BidResponsePostProcessor {

    private final BidResponseSenderClient bidResponseSenderClient;

    public BidResponseSender(BidResponseSenderClient bidResponseSenderClient) {
        this.bidResponseSenderClient = bidResponseSenderClient;
    }

    @Override
    public Future<BidResponse> postProcess(
            HttpRequestContext httpRequest,
            UidsCookie uidsCookie,
            BidRequest bidRequest,
            BidResponse bidResponse,
            Account account) {
        // Prepare required data and send to an external server
        bidResponseSenderClient.sendDataToServer(bidResponse);
        return null;
    }
}
