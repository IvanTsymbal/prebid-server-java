package org.prebid.server.hooks.modules.com.confiant.adquality.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreativeAttributes {

    /** Array containing a list of brands. */
    List<String> brands;

    /** Array containing a list of categories names and codes {@link CreativeCategory}. */
    List<CreativeCategory> categories;
}
