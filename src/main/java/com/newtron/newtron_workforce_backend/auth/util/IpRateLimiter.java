package com.newtron.newtron_workforce_backend.auth.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IpRateLimiter {

    private final Map<String, IpRequestTracker> trackerMap = new ConcurrentHashMap<>();

    public boolean isAllowed(String ipAddress, int limit, long windowMillis) {
        if ("127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress) || "localhost".equals(ipAddress)) {
            return true;
        }
        long now = System.currentTimeMillis();
        IpRequestTracker tracker = trackerMap.computeIfAbsent(ipAddress, k -> new IpRequestTracker(now));
        
        synchronized (tracker) {
            if (now - tracker.windowStart > windowMillis) {
                tracker.windowStart = now;
                tracker.count.set(1);
                return true;
            }
            return tracker.count.incrementAndGet() <= limit;
        }
    }

    private static class IpRequestTracker {
        volatile long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        IpRequestTracker(long start) {
            this.windowStart = start;
        }
    }
}
