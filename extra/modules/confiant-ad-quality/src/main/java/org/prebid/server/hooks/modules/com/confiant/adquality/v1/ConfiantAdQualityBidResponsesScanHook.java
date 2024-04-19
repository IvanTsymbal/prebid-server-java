package org.prebid.server.hooks.modules.com.confiant.adquality.v1;

import com.iab.openrtb.request.BidRequest;
import com.iab.openrtb.request.Device;
import com.iab.openrtb.request.User;
import io.vertx.core.Future;
import org.prebid.server.activity.Activity;
import org.prebid.server.activity.ComponentType;
import org.prebid.server.activity.infrastructure.payload.ActivityInvocationPayload;
import org.prebid.server.activity.infrastructure.payload.impl.ActivityInvocationPayloadImpl;
import org.prebid.server.activity.infrastructure.payload.impl.BidRequestActivityInvocationPayload;
import org.prebid.server.auction.model.AuctionContext;
import org.prebid.server.auction.model.BidderResponse;
import org.prebid.server.auction.privacy.enforcement.mask.UserFpdActivityMask;
import org.prebid.server.hooks.execution.v1.bidder.AllProcessedBidResponsesPayloadImpl;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.AnalyticsMapper;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.BidsScanResultProcessor;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.BidsMapper;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.BidsScanResult;
import org.prebid.server.hooks.modules.com.confiant.adquality.core.BidsScanner;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.ScanResultStatus;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.model.InvocationResultImpl;
import org.prebid.server.hooks.v1.InvocationAction;
import org.prebid.server.hooks.v1.InvocationResult;
import org.prebid.server.hooks.v1.InvocationStatus;
import org.prebid.server.hooks.v1.auction.AuctionInvocationContext;
import org.prebid.server.hooks.v1.bidder.AllProcessedBidResponsesHook;
import org.prebid.server.hooks.v1.bidder.AllProcessedBidResponsesPayload;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfiantAdQualityBidResponsesScanHook implements AllProcessedBidResponsesHook {

    private static final String CODE = "confiant-ad-quality-bid-responses-scan-hook";

    private final BidsScanner bidsScanner;
    private final BidsScanResultProcessor bidsScanResultProcessor;
    private final List<String> biddersToExcludeFromScan;
    private final UserFpdActivityMask userFpdActivityMask;

    public ConfiantAdQualityBidResponsesScanHook(BidsScanner bidsScanner,
                                                 BidsScanResultProcessor bidsScanResultProcessor,
                                                 List<String> biddersToExcludeFromScan,
                                                 UserFpdActivityMask userFpdActivityMask) {

        this.bidsScanner = Objects.requireNonNull(bidsScanner);
        this.bidsScanResultProcessor = Objects.requireNonNull(bidsScanResultProcessor);
        this.biddersToExcludeFromScan = Objects.requireNonNull(biddersToExcludeFromScan);
        this.userFpdActivityMask = Objects.requireNonNull(userFpdActivityMask);
    }

    @Override
    public Future<InvocationResult<AllProcessedBidResponsesPayload>> call(
            AllProcessedBidResponsesPayload allProcessedBidResponsesPayload,
            AuctionInvocationContext auctionInvocationContext) {

        final BidRequest bidRequest = getBidRequest(auctionInvocationContext);
        final List<BidderResponse> responses = allProcessedBidResponsesPayload.bidResponses();
        final Map<Boolean, List<BidderResponse>> needScanMap = responses.stream()
                .collect(Collectors.groupingBy(this::isScanRequired));

        final List<BidderResponse> toScan = needScanMap.getOrDefault(true, Collections.emptyList());
        final List<BidderResponse> avoidScan = needScanMap.getOrDefault(false, Collections.emptyList());

        return bidsScanner.submitBids(BidsMapper.toRedisBidsFromBidResponses(bidRequest, toScan))
                .map(scanResult -> toInvocationResult(scanResult, toScan, avoidScan, auctionInvocationContext));
    }

    private boolean isScanRequired(BidderResponse bidderResponse) {
        return !biddersToExcludeFromScan.contains(bidderResponse.getBidder())
                && !bidderResponse.getSeatBid().getBids().isEmpty();
    }

    private BidRequest getBidRequest(AuctionInvocationContext auctionInvocationContext) {
        final AuctionContext auctionContext = auctionInvocationContext.auctionContext();
        final BidRequest bidRequest = auctionContext.getBidRequest();
        final ActivityInvocationPayload activityInvocationPayload = BidRequestActivityInvocationPayload.of(
                ActivityInvocationPayloadImpl.of(ComponentType.GENERAL_MODULE, ConfiantAdQualityModule.CODE),
                bidRequest);
        final boolean disallowTransmitGeo = !auctionContext.getActivityInfrastructure()
                .isAllowed(Activity.TRANSMIT_GEO, activityInvocationPayload);

        final User maskedUser = userFpdActivityMask.maskUser(bidRequest.getUser(), true, true, disallowTransmitGeo);
        final Device maskedDevice = userFpdActivityMask.maskDevice(bidRequest.getDevice(), true, disallowTransmitGeo);

        return bidRequest.toBuilder()
                .user(maskedUser)
                .device(maskedDevice)
                .build();
    }

    private InvocationResult<AllProcessedBidResponsesPayload> toInvocationResult(
            BidsScanResult bidsScanResult,
            List<BidderResponse> scannedBidderResponses,
            List<BidderResponse> notScannedBidderResponses,
            AuctionInvocationContext auctionInvocationContext) {

        final boolean debugEnabled = auctionInvocationContext.debugEnabled();
        final Map<ScanResultStatus, List<BidderResponse>> mapByStatus = bidsScanResultProcessor
                .toMapByStatus(scannedBidderResponses, notScannedBidderResponses, bidsScanResult);

        final boolean shouldUpdate = !mapByStatus.get(ScanResultStatus.HAS_ISSUE).isEmpty()
                || !mapByStatus.get(ScanResultStatus.BLOCKED_BRAND).isEmpty()
                || !mapByStatus.get(ScanResultStatus.BLOCKED_CATEGORY).isEmpty();

        final InvocationResultImpl.InvocationResultImplBuilder<AllProcessedBidResponsesPayload> resultBuilder =
                InvocationResultImpl.<AllProcessedBidResponsesPayload>builder()
                        .status(InvocationStatus.success)
                        .action(shouldUpdate
                                ? InvocationAction.update
                                : InvocationAction.no_action)
                        .errors(shouldUpdate
                                ? bidsScanResult.getIssuesMessages()
                                : null)
                        .debugMessages(debugEnabled
                                ? bidsScanResult.getDebugMessages()
                                : null)
                        .analyticsTags(AnalyticsMapper.toAnalyticsTags(mapByStatus))
                        .payloadUpdate(payload -> AllProcessedBidResponsesPayloadImpl.of(toSafeBidderResponses(mapByStatus)));

        return resultBuilder.build();
    }

    private List<BidderResponse> toSafeBidderResponses(Map<ScanResultStatus, List<BidderResponse>> mapByStatus) {
        return Stream.of(
                mapByStatus.get(ScanResultStatus.NO_ISSUES),
                mapByStatus.get(ScanResultStatus.DISALLOWED_BRAND),
                mapByStatus.get(ScanResultStatus.DISALLOWED_CATEGORY),
                mapByStatus.get(ScanResultStatus.NOT_SCANNED))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public String code() {
        return CODE;
    }
}
