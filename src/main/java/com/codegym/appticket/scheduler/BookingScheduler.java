    package com.codegym.appticket.scheduler;

    import com.codegym.appticket.service.IBookingService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Component;

    @Component
    @RequiredArgsConstructor
    public class BookingScheduler {

        private final IBookingService bookingService;

        // Run every minute
        @Scheduled(fixedRate = 60000)
        public void cleanupExpiredBookings() {
            bookingService.expireBookings();
        }
    }
