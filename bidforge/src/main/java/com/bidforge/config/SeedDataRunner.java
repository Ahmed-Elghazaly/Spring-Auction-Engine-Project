package com.bidforge.config;

import com.bidforge.entity.*;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.entity.enums.RoleName;
import com.bidforge.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Seeded admin account: admin / Admin@123
// Seeded user accounts: seller1, bidder1, bidder2 / Password@123

@Component
public class SeedDataRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private static final String DEMO_PASSWORD = "Password@123";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataRunner(RoleRepository roleRepository,
                          UserRepository userRepository,
                          AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          AuctionResultRepository auctionResultRepository,
                          PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.auctionResultRepository = auctionResultRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdmin();
        seedDemoUsers();
        seedDemoAuctions();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
                log.info("Seeded role {}", roleName);
            }
        }
    }


    // one administrator account
    // There is no API that grants admin role, public registration always produces normal users
    // seeding is the only way to get the admin role
    private void seedAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        User admin = new User("admin", "admin@bidforge.local",
                passwordEncoder.encode("Admin@123"), "Platform", "Admin");
        admin.addRole(roleRepository.findByName(RoleName.ROLE_USER).orElseThrow());
        admin.addRole(roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow());
        userRepository.save(admin);
        log.info("Seeded admin account 'admin'");
    }

    // 3 demo users
    private void seedDemoUsers() {
        seedUserIfMissing("seller1", "seller1@bidforge.local", "Sara", "Ahmed");
        seedUserIfMissing("bidder1", "bidder1@bidforge.local", "Badr", "Kamel");
        seedUserIfMissing("bidder2", "bidder2@bidforge.local", "Basma", "Mohammed");
    }

    private void seedUserIfMissing(String username, String email, String firstName, String lastName) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User(username, email, passwordEncoder.encode(DEMO_PASSWORD), firstName, lastName);
        user.addRole(roleRepository.findByName(RoleName.ROLE_USER).orElseThrow());
        userRepository.save(user);
        log.info("Seeded demo user '{}'", username);
    }


    // One auction in every lifecycle state, with old dates
    private void seedDemoAuctions() {
        if (auctionRepository.count() > 0) {
            return; // we don't touch a database that already has auctions
        }
        User seller = userRepository.findByUsername("seller1").orElseThrow();
        User bidder1 = userRepository.findByUsername("bidder1").orElseThrow();
        User bidder2 = userRepository.findByUsername("bidder2").orElseThrow();
        Instant now = Instant.now();

        // SCHEDULED English, opens tomorrow
        Auction scheduled = new Auction(
                "Rolex Watch", "Old Rolex Watch",
                AuctionCategory.COLLECTIBLES, AuctionType.ENGLISH,
                new BigDecimal("5000.00"), new BigDecimal("100.00"),
                now.plus(1, ChronoUnit.DAYS), now.plus(3, ChronoUnit.DAYS), seller);
        auctionRepository.save(scheduled);

        // OPEN English with some bids
        Auction openEnglish = new Auction(
                "Gaming laptop RTX 5090", "Barely used, 32GB RAM, 2TB SSD, warranty until 2027.",
                AuctionCategory.ELECTRONICS, AuctionType.ENGLISH,
                new BigDecimal("1500.00"), new BigDecimal("50.00"),
                now.minus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.DAYS), seller);
        openEnglish.setStatus(AuctionStatus.OPEN);
        openEnglish.setCreatedAt(now.minus(3, ChronoUnit.HOURS));
        auctionRepository.save(openEnglish);
        saveBid(openEnglish, bidder1, "1500.00", now.minus(90, ChronoUnit.MINUTES));
        saveBid(openEnglish, bidder2, "1550.00", now.minus(60, ChronoUnit.MINUTES));
        openEnglish.setCurrentHighestBid(new BigDecimal("1550.00"));

        // OPEN sealed with one bid
        Auction openSealed = new Auction(
                "Harry Potter Books Collection", "Signed by the author",
                AuctionCategory.BOOKS, AuctionType.SEALED_BID,
                new BigDecimal("300.00"), null,
                now.minus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.DAYS), seller);
        openSealed.setStatus(AuctionStatus.OPEN);
        openSealed.setCreatedAt(now.minus(2, ChronoUnit.HOURS));
        auctionRepository.save(openSealed);
        saveBid(openSealed, bidder1, "350.00", now.minus(30, ChronoUnit.MINUTES));

        // CLOSED English with a winner
        Auction closed = new Auction(
                "Oak wood writing desk", "From the late 19th century, restored",
                AuctionCategory.ART, AuctionType.ENGLISH,
                new BigDecimal("400.00"), new BigDecimal("25.00"),
                now.minus(3, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), seller);
        closed.setStatus(AuctionStatus.CLOSED);
        closed.setCreatedAt(now.minus(4, ChronoUnit.DAYS));
        auctionRepository.save(closed);
        saveBid(closed, bidder2, "400.00", now.minus(2, ChronoUnit.DAYS));
        Bid winning = saveBid(closed, bidder1, "450.00", now.minus(36, ChronoUnit.HOURS));
        closed.setCurrentHighestBid(new BigDecimal("450.00"));
        auctionResultRepository.save(new AuctionResult(
                closed, bidder1, winning, winning.getAmount(), now.minus(1, ChronoUnit.DAYS)));

        // CANCELLED auction
        Auction cancelled = new Auction(
                "Motorcycle", "Hydraulic brakes",
                AuctionCategory.SPORTS, AuctionType.ENGLISH,
                new BigDecimal("800.00"), new BigDecimal("20.00"),
                now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS), seller);
        cancelled.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(cancelled);

        log.info("Seeded 5 demo auctions (SCHEDULED, OPEN english, OPEN sealed, CLOSED, CANCELLED)");
    }

    private Bid saveBid(Auction auction, User bidder, String amount, Instant at) {
        Bid bid = new Bid(new BigDecimal(amount), auction, bidder);
        bid.setCreatedAt(at);
        return bidRepository.save(bid);
    }
}
