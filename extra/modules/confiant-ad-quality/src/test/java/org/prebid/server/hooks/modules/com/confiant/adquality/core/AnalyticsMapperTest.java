package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import org.junit.Test;
import org.prebid.server.auction.model.BidderResponse;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.AnalyticsTag;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.ScanResultStatus;
import org.prebid.server.hooks.modules.com.confiant.adquality.util.AdQualityModuleTestUtils;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.model.analytics.AppliedToImpl;
import org.prebid.server.hooks.modules.com.confiant.adquality.v1.model.analytics.ResultImpl;
import org.prebid.server.hooks.v1.analytics.Tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AnalyticsMapperTest {

    @Test
    public void toAnalyticsTagsShouldMapBidsScanResultToAnalyticsTags() {
        // given
        final List<BidderResponse> bidderResponsesWithIssues = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_a", "imp_a", "bid_id_a"));

        final List<BidderResponse> bidderResponsesWithBlockedBrand = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_b", "imp_b", "bid_id_b"));

        final List<BidderResponse> bidderResponsesWithBlockedCategory = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_c", "imp_c", "bid_id_c"));

        final List<BidderResponse> bidderResponsesWithDisallowedBrand = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_d", "imp_d", "bid_id_d"));

        final List<BidderResponse> bidderResponsesWithDisallowedCategory = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_e", "imp_e", "bid_id_e"));

        final List<BidderResponse> bidderResponsesWithoutIssues = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_f", "imp_f", "bid_id_f"));

        final List<BidderResponse> bidderResponsesNotScanned = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_g", "imp_g", "bid_id_g"));

        final Map<ScanResultStatus, List<BidderResponse>> mapByStatus = new HashMap<>();
        mapByStatus.put(ScanResultStatus.HAS_ISSUE, bidderResponsesWithIssues);
        mapByStatus.put(ScanResultStatus.BLOCKED_BRAND, bidderResponsesWithBlockedBrand);
        mapByStatus.put(ScanResultStatus.BLOCKED_CATEGORY, bidderResponsesWithBlockedCategory);
        mapByStatus.put(ScanResultStatus.DISALLOWED_BRAND, bidderResponsesWithDisallowedBrand);
        mapByStatus.put(ScanResultStatus.DISALLOWED_CATEGORY, bidderResponsesWithDisallowedCategory);
        mapByStatus.put(ScanResultStatus.NO_ISSUES, bidderResponsesWithoutIssues);
        mapByStatus.put(ScanResultStatus.NOT_SCANNED, bidderResponsesNotScanned);


        // when
        final Tags tags = AnalyticsMapper.toAnalyticsTags(mapByStatus);

        // then
        assertThat(tags.activities().size()).isEqualTo(1);
        assertThat(tags.activities().get(0).name()).isEqualTo("ad-scan");
        assertThat(tags.activities().get(0).status()).isEqualTo("success");
        assertThat(tags.activities().get(0).results()).containsExactlyInAnyOrderElementsOf(List.of(
                ResultImpl.of(AnalyticsTag.INSPECTED_HAS_ISSUE.toString(), null, AppliedToImpl.builder()
                        .bidders(List.of("bidder_a"))
                        .impIds(List.of("imp_a"))
                        .bidIds(List.of("bid_id_a"))
                        .build()),
                ResultImpl.of(AnalyticsTag.BLOCKED_BRAND_FOUND.toString(), null, AppliedToImpl.builder()
                        .bidders(List.of("bidder_b"))
                        .impIds(List.of("imp_b"))
                        .bidIds(List.of("bid_id_b"))
                        .build()),
                ResultImpl.of(AnalyticsTag.BLOCKED_CATEGORY_FOUND.toString(), null, AppliedToImpl.builder()
                        .bidders(List.of("bidder_c"))
                        .impIds(List.of("imp_c"))
                        .bidIds(List.of("bid_id_c"))
                        .build()),
                ResultImpl.of(AnalyticsTag.DISALLOWED_BRAND_FOUND.toString(), null, AppliedToImpl.builder()
                        .bidders(List.of("bidder_d"))
                        .impIds(List.of("imp_d"))
                        .bidIds(List.of("bid_id_d"))
                        .build()),
                ResultImpl.of(AnalyticsTag.DISALLOWED_CATEGORY_FOUND.toString(), null, AppliedToImpl.builder()
                        .bidders(List.of("bidder_e"))
                        .impIds(List.of("imp_e"))
                        .bidIds(List.of("bid_id_e"))
                        .build()),
                ResultImpl.of(AnalyticsTag.INSPECTED_NO_ISSUES.toString(), null, AppliedToImpl.builder()
                        .bidders(List.of("bidder_f"))
                        .impIds(List.of("imp_f"))
                        .bidIds(List.of("bid_id_f"))
                        .build()),
                ResultImpl.of(AnalyticsTag.SKIPPED.toString(), null, AppliedToImpl.builder()
                        .bidders(List.of("bidder_g"))
                        .impIds(List.of("imp_g"))
                        .bidIds(List.of("bid_id_g"))
                        .build())));
    }
}
