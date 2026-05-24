package com.eyesecurity.service;

import com.eyesecurity.common.AnalyticsEvent;

import java.util.List;

public interface AnalyticsClient {
    void submit(List<AnalyticsEvent> events);
}
