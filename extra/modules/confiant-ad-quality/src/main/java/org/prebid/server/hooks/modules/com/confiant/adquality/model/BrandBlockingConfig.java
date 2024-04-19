package org.prebid.server.hooks.modules.com.confiant.adquality.model;

import lombok.Data;

@Data
public class BrandBlockingConfig {

    /** Confiant's Url to get brand and category blocking detection configuration */
    String configUrl;

    /** Time interval in milliseconds to reload brand and category blocking detection configuration */
    int stateCheckInterval;

    /** Time interval in milliseconds to wait for a response of the configuration */
    Long timeout;
}
