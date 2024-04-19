package org.prebid.server.hooks.modules.com.confiant.adquality.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class BrandBlockingData {

    List<String> categories;

    List<String> brands;

    boolean blocking;
}
