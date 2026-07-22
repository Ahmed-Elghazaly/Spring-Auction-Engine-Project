package com.bidforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


// @EnableScheduling switches on Spring's scheduler so the method annotated with @Scheduled in AuctionSchedulerService runs periodically
// it auto-opens and auto-closes auctions whose time arrived
@SpringBootApplication
@EnableScheduling
public class BidforgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BidforgeApplication.class, args);
    }

}
