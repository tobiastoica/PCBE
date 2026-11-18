package com.fleet.station.service;

import org.springframework.stereotype.Service;

@Service
public class FareCalculatorService {

    public static final int BASE_UNLOCK_FEE_CENTS = 100; // 1.00 EUR
    public static final int ACTIVE_RATE_CENTS_PER_MINUTE = 25; // 0.25 EUR/min
    public static final int PAUSE_RATE_CENTS_PER_MINUTE = 10;  // 0.10 EUR/min

    public int calculateFare(long activeSeconds, long pauseSeconds) {
        if (activeSeconds <= 0 && pauseSeconds <= 0) {
            return 0;
        }

        long activeMinutes = (activeSeconds + 59) / 60;
        long pauseMinutes = (pauseSeconds + 59) / 60;

        return (int) (BASE_UNLOCK_FEE_CENTS 
                + (activeMinutes * ACTIVE_RATE_CENTS_PER_MINUTE) 
                + (pauseMinutes * PAUSE_RATE_CENTS_PER_MINUTE));
    }
}
