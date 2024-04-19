package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import org.prebid.server.auction.model.BidderResponse;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BidScanResult;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BrandBlockingData;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.ScanResultStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BidsScanResultProcessor {

    private final Map<String, Boolean> brandToBlockMap = new HashMap<>();

    private final Map<String, Boolean> brandToDetectMap = new HashMap<>();

    private final Map<String, Boolean> categoryToBlockMap = new HashMap<>();

    private final Map<String, Boolean> categoryToDetectMap = new HashMap<>();

    public void setBrandBlockingData(List<BrandBlockingData> brandBlockingData) {
        resetBrandBlockingData();
        brandBlockingData.forEach(data -> {
            if (data.isBlocking()) {
                data.getBrands().forEach(brand -> brandToBlockMap.put(brand, true));
                data.getCategories().forEach(cat -> categoryToBlockMap.put(cat, true));
            } else {
                data.getBrands().forEach(brand -> brandToDetectMap.put(brand, true));
                data.getCategories().forEach(cat -> categoryToDetectMap.put(cat, true));
            }
        });
    }

    public Map<ScanResultStatus, List<BidderResponse>> toMapByStatus(
            List<BidderResponse> scannedBidderResponses,
            List<BidderResponse> notScannedBidderResponses,
            BidsScanResult bidsScanResult) {

        final Map<ScanResultStatus, List<BidderResponse>> scanResultMap = new HashMap<>();

        List<BidderResponse> withIssues = new ArrayList<>();
        List<BidderResponse> withBlockedBrands = new ArrayList<>();
        List<BidderResponse> withBlockedCats = new ArrayList<>();
        List<BidderResponse> withDisallowedBrands = new ArrayList<>();
        List<BidderResponse> withDisallowedCats = new ArrayList<>();
        List<BidderResponse> withoutIssues = new ArrayList<>();

        final int groupSize = scannedBidderResponses.size();

        for (int i = 0; i < groupSize; i++) {
            final BidderResponse bidderResponse = scannedBidderResponses.get(i);
            final BidScanResult bidScanResult = bidsScanResult.getBidScanResults().size() > i
                    ? bidsScanResult.getBidScanResults().get(i)
                    : null;
            if (hasIssues(bidScanResult)) {
                withIssues.add(bidderResponse);
            } else if (hasBlockedBrand(bidScanResult)) {
                withBlockedBrands.add(bidderResponse);
            } else if (hasBlockedCategory(bidScanResult)) {
                withBlockedCats.add(bidderResponse);
            } else if (hasDisallowedBrand(bidScanResult)) {
                withDisallowedBrands.add(bidderResponse);
            } else if (hasDisallowedCategory(bidScanResult)) {
                withDisallowedCats.add(bidderResponse);
            } else {
                withoutIssues.add(bidderResponse);
            }
        }

        scanResultMap.put(ScanResultStatus.HAS_ISSUE, withIssues);
        scanResultMap.put(ScanResultStatus.BLOCKED_BRAND, withBlockedBrands);
        scanResultMap.put(ScanResultStatus.BLOCKED_CATEGORY, withBlockedCats);
        scanResultMap.put(ScanResultStatus.DISALLOWED_BRAND, withDisallowedBrands);
        scanResultMap.put(ScanResultStatus.DISALLOWED_CATEGORY, withDisallowedCats);
        scanResultMap.put(ScanResultStatus.NO_ISSUES, withoutIssues);
        scanResultMap.put(ScanResultStatus.NOT_SCANNED, notScannedBidderResponses);

        return scanResultMap;
    }

    private boolean hasIssues(BidScanResult bidScanResult) {
        return bidScanResult != null && bidScanResult.getIssues() != null && !bidScanResult.getIssues().isEmpty();
    }

    private boolean hasBlockedBrand(BidScanResult bidScanResult) {
        return hasBrandInMap(bidScanResult, brandToBlockMap);
    }

    private boolean hasDisallowedBrand(BidScanResult bidScanResult) {
        return hasBrandInMap(bidScanResult, brandToDetectMap);
    }

    private boolean hasBrandInMap(BidScanResult bidScanResult, Map<String, Boolean> brandMap) {
        return bidScanResult != null
                && bidScanResult.getAttributes() != null
                && bidScanResult.getAttributes().getBrands() != null
                && bidScanResult.getAttributes().getBrands().stream().anyMatch(brand -> brandMap.get(brand) != null);
    }

    private boolean hasBlockedCategory(BidScanResult bidScanResult) {
        return hasCategoryInMap(bidScanResult, categoryToBlockMap);
    }

    private boolean hasDisallowedCategory(BidScanResult bidScanResult) {
        return hasCategoryInMap(bidScanResult, categoryToDetectMap);
    }

    private boolean hasCategoryInMap(BidScanResult bidScanResult, Map<String, Boolean> catMap) {
        return bidScanResult != null
                && bidScanResult.getAttributes() != null
                && bidScanResult.getAttributes().getCategories() != null
                && bidScanResult.getAttributes().getCategories().stream().anyMatch(cat -> catMap.get(cat.getCode()) != null);
    }

    private void resetBrandBlockingData() {
        brandToBlockMap.clear();
        brandToDetectMap.clear();
        categoryToBlockMap.clear();
        categoryToDetectMap.clear();
    }
}
