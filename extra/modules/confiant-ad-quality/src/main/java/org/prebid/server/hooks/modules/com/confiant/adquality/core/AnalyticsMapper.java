package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import com.iab.openrtb.response.Bid;
import org.prebid.server.auction.model.BidderResponse;
import org.prebid.server.bidder.model.BidderBid;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.AnalyticsTag;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.ScanResultStatus;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.model.analytics.ActivityImpl;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.model.analytics.AppliedToImpl;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.model.analytics.ResultImpl;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.model.analytics.TagsImpl;
import org.prebid.server.hooks.v1.analytics.AppliedTo;
import org.prebid.server.hooks.v1.analytics.Result;
import org.prebid.server.hooks.v1.analytics.Tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsMapper {

    private static final String AD_QUALITY_SCAN = "ad-scan";
    private static final String SUCCESS_STATUS = "success";

    public static Tags toAnalyticsTags(Map<ScanResultStatus, List<BidderResponse>> mapByStatus) {

        return TagsImpl.of(Collections.singletonList(ActivityImpl.of(
                AD_QUALITY_SCAN,
                SUCCESS_STATUS,
                toActivityResults(mapByStatus))));
    }

    private static List<Result> toActivityResults(Map<ScanResultStatus, List<BidderResponse>> mapByStatus) {

        final List<Result> results = new ArrayList<>();

        mapByStatus.forEach((status, responses) -> {
            if (!responses.isEmpty()) {
                results.add(ResultImpl.of(StatusTagMapper.toTag(status).toString(), null, toAppliedTo(responses)));
            }
        });

        return results;
    }


    private static class StatusTagMapper {

        private static final EnumMap<ScanResultStatus, AnalyticsTag> STATUS_TO_TAG =
                new EnumMap<>(ScanResultStatus.class);

        static {
            STATUS_TO_TAG.put(ScanResultStatus.HAS_ISSUE, AnalyticsTag.INSPECTED_HAS_ISSUE);
            STATUS_TO_TAG.put(ScanResultStatus.BLOCKED_BRAND, AnalyticsTag.BLOCKED_BRAND_FOUND);
            STATUS_TO_TAG.put(ScanResultStatus.BLOCKED_CATEGORY, AnalyticsTag.BLOCKED_CATEGORY_FOUND);
            STATUS_TO_TAG.put(ScanResultStatus.DISALLOWED_BRAND, AnalyticsTag.DISALLOWED_BRAND_FOUND);
            STATUS_TO_TAG.put(ScanResultStatus.DISALLOWED_CATEGORY, AnalyticsTag.DISALLOWED_CATEGORY_FOUND);
            STATUS_TO_TAG.put(ScanResultStatus.NO_ISSUES, AnalyticsTag.INSPECTED_NO_ISSUES);
            STATUS_TO_TAG.put(ScanResultStatus.NOT_SCANNED, AnalyticsTag.SKIPPED);
        }

        static AnalyticsTag toTag(ScanResultStatus status) {
            return STATUS_TO_TAG.getOrDefault(status, AnalyticsTag.SKIPPED);
        }
    }

    private static AppliedTo toAppliedTo(List<BidderResponse> bidderResponses) {
        final List<Bid> bids = toBids(bidderResponses);
        return AppliedToImpl.builder()
                .bidders(bidderResponses.stream().map(BidderResponse::getBidder).toList())
                .impIds(bids.stream().map(Bid::getImpid).toList())
                .bidIds(bids.stream().map(Bid::getId).toList())
                .build();
    }

    private static List<Bid> toBids(List<BidderResponse> bidderResponses) {
        return bidderResponses.stream()
                .map(BidderResponse::getSeatBid)
                .flatMap(seatBid -> seatBid.getBids().stream())
                .map(BidderBid::getBid)
                .collect(Collectors.toList());
    }
}
