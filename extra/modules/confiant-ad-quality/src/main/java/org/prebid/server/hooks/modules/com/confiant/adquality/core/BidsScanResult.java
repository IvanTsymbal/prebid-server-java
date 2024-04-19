package org.prebid.server.hooks.modules.com.confiant.adquality.core;

import lombok.Builder;
import lombok.Value;
import org.prebid.server.hooks.modules.com.confiant.adquality.model.BidScanResult;

import java.util.List;

@Builder
@Value(staticConstructor = "of")
public class BidsScanResult {

    List<BidScanResult> bidScanResults;

    List<String> debugMessages;

    public List<String> getIssuesMessages() {
        return bidScanResults.stream()
                .map(r -> r.getTagKey() + ": " + (r.getIssues() == null ? "no issues" : r.getIssues().toString()))
                .toList();
    }
}
