package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.prebid.server.auction.model.BidderResponse;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BrandBlockingData;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.ScanResultStatus;
import org.prebid.server.hooks.modules.com.confiant.adquality.util.AdQualityModuleTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BidsScanResultProcessorTest {

    private final RedisParser redisParser = new RedisParser(new ObjectMapper());

    private BidsScanResultProcessor target;

    @Before
    public void setUp() {
        target = new BidsScanResultProcessor();
    }

    @Test
    public void toMapByStatusShouldProperlyMapBidderResponses() {
        // given
        final String brandToBlock = "brandToBlock";
        final String catToBlock = "catToBlock";
        final String brandToDetect = "brandToDetect";
        final String catToDetect = "brandToDetect";

        target.setBrandBlockingData(List.of(
                BrandBlockingData.builder()
                        .blocking(true)
                        .brands(List.of(brandToBlock))
                        .categories(List.of(catToBlock))
                        .build(),
                BrandBlockingData.builder()
                        .blocking(false)
                        .brands(List.of(brandToDetect))
                        .categories(List.of(catToDetect))
                        .build()));

        final List<BidderResponse> scannedBidderResponses = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_a", "imp_a", "bid_id_a"),
                AdQualityModuleTestUtils.getBidderResponse("bidder_b", "imp_a", "bid_id_a"),
                AdQualityModuleTestUtils.getBidderResponse("bidder_c", "imp_a", "bid_id_a"),
                AdQualityModuleTestUtils.getBidderResponse("bidder_d", "imp_a", "bid_id_a"),
                AdQualityModuleTestUtils.getBidderResponse("bidder_r", "imp_a", "bid_id_a"),
                AdQualityModuleTestUtils.getBidderResponse("bidder_f", "imp_a", "bid_id_a"));

        final List<BidderResponse> notScannedBidderResponses = List.of(
                AdQualityModuleTestUtils.getBidderResponse("bidder_n", "imp_a", "bid_id_a"));

        final BidsScanResult bidsScanResult = redisParser.parseBidsScanResult(
                "[[" +
                        "[{\"tag_key\": \"tag_a\", \"imp_id\": \"imp_a\", \"issues\": [{\"spec_name\": \"malicious_domain\", \"value\": \"ads.deceivenetworks.net\", \"first_adinstance\": \"e91e8da982bb8b7f80100426\"}]}]," +
                        "[{\"tag_key\": \"tag_b\", \"imp_id\": \"imp_b\", \"issues\": [], \"attributes\": {\"brands\": [\"" + brandToBlock + "\"], \"categories\": [{\"code\": \"cat\", \"name\": \"Just name\"}]}}]," +
                        "[{\"tag_key\": \"tag_c\", \"issues\": [], \"attributes\": {\"categories\": [{\"code\": \"" + catToBlock + "\", \"name\": \"Just name\"}]}}]," +
                        "[{\"tag_key\": \"tag_d\", \"issues\": [], \"attributes\": {\"brands\": [\"" + brandToDetect + "\"]}}]," +
                        "[{\"tag_key\": \"tag_e\", \"issues\": [], \"attributes\": {\"categories\": [{\"code\": \"" + catToDetect + "\", \"name\": \"Just name\"}]}}]," +
                        "[{\"tag_key\": \"tag_f\", \"issues\": []}]" +
                    "]]");

        // when
        final Map<ScanResultStatus, List<BidderResponse>> mapByStatus = target.toMapByStatus(
                scannedBidderResponses, notScannedBidderResponses, bidsScanResult);

        // then
        assertThat(mapByStatus.get(ScanResultStatus.HAS_ISSUE)).containsExactly(scannedBidderResponses.get(0));
        assertThat(mapByStatus.get(ScanResultStatus.BLOCKED_BRAND)).containsExactly(scannedBidderResponses.get(1));
        assertThat(mapByStatus.get(ScanResultStatus.BLOCKED_CATEGORY)).containsExactly(scannedBidderResponses.get(2));
        assertThat(mapByStatus.get(ScanResultStatus.DISALLOWED_BRAND)).containsExactly(scannedBidderResponses.get(3));
        assertThat(mapByStatus.get(ScanResultStatus.DISALLOWED_CATEGORY)).containsExactly(scannedBidderResponses.get(4));
        assertThat(mapByStatus.get(ScanResultStatus.NO_ISSUES)).containsExactly(scannedBidderResponses.get(5));
        assertThat(mapByStatus.get(ScanResultStatus.NOT_SCANNED)).containsExactly(notScannedBidderResponses.get(0));
    }
}
