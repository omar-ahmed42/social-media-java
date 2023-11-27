package com.omarahmed42.socialmedia.service;

public interface StatisticsService {
    void increment(String id, String activityType, Long value);
    void increment(String id, String activityType);

    void decrement(String id, String activityType, Long value);
    void decrement(String id, String activityType);
    
    Long get(String id, String activityType);
    Object getStatistics(String id);
}
