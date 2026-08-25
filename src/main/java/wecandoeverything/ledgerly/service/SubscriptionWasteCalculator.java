package wecandoeverything.ledgerly.service;

import org.springframework.stereotype.Component;
import wecandoeverything.ledgerly.dto.SubscriptionDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SubscriptionWasteCalculator {

    public int idleSeats(SubscriptionDto s) {
        return s.getSeats() - s.getActiveSeats();
    }

    public BigDecimal monthlyWaste(SubscriptionDto s) {
        return s.getMonthlyCost()
                .divide(BigDecimal.valueOf(s.getSeats()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(idleSeats(s)));
    }

    public String severity(SubscriptionDto s) {
        double utilization = (double) s.getActiveSeats() / s.getSeats();
        if (utilization < 0.4) return "high";
        if (utilization < 0.7) return "medium";
        return "low";
    }
}