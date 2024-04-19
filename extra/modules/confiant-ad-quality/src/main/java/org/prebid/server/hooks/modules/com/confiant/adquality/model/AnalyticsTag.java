package org.prebid.server.hooks.modules.com.confiant.adquality.model;

public enum AnalyticsTag {

    INSPECTED_HAS_ISSUE("inspected-has-issue"),
    INSPECTED_NO_ISSUES("inspected-no-issues"),
    DISALLOWED_CATEGORY_FOUND("disallowed-category-found"),
    DISALLOWED_BRAND_FOUND("disallowed-brand-found"),
    BLOCKED_CATEGORY_FOUND("blocked-category-found"),
    BLOCKED_BRAND_FOUND("blocked-brand-found"),

    SKIPPED("skipped");

    private final String name;

    AnalyticsTag(String name) {
        this.name = name;
    }

    AnalyticsTag() {
        this.name = name();
    }

    @Override
    public String toString() {
        return name;
    }
}
