package com.bidforge.config;

import com.bidforge.entity.*;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.entity.enums.RoleName;
import com.bidforge.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Seeded admin account: admin / Admin@123
// Seeded user accounts: sara, omar, layla, youssef / Password@123
//   sara + omar sell, layla + youssef bid.

// ROLES are reference data the application cannot run without, so they are always seeded, in every profile

// The demo accounts and sample auctions are development conveniences with publicly known passwords so they are seeded ONLY when
// bidforge.seed.demo-data is true (dev profile), The prod profile sets it to false so a real deployment can never hand out an admin login.

@Component
public class SeedDataRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private static final String DEMO_PASSWORD = "Password@123";

    // Defaults to false: if a profile forgets to mention it, the SAFE option wins.
    @Value("${bidforge.seed.demo-data:false}")
    private boolean seedDemoData;

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

        if (!seedDemoData) {
            log.info("Demo data seeding is disabled (bidforge.seed.demo-data=false)");
            return;
        }

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

    // Four demo users. Two mostly sell, two mostly bid, but the roles are not
    // enforced anywhere: any account can list an auction and bid on someone
    // else's. The split just makes the sample data read like a real market.
    private void seedDemoUsers() {
        seedUserIfMissing("sara", "sara@bidforge.local", "Sara", "Ahmed");
        seedUserIfMissing("omar", "omar@bidforge.local", "Omar", "Khalil");
        seedUserIfMissing("layla", "layla@bidforge.local", "Layla", "Hassan");
        seedUserIfMissing("youssef", "youssef@bidforge.local", "Youssef", "Nabil");
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


    /**
     * Seven sample auctions covering every lifecycle state and both formats.
     *
     * <p>The data is arranged so that signing in as <b>layla</b> shows something
     * on every screen: she is winning one auction, has been outbid on another,
     * has a sealed bid in progress, and has won two auctions that already
     * closed (one of them sealed, where the price she paid is higher than the
     * starting price).
     */
    private void seedDemoAuctions() {
        if (auctionRepository.count() > 0) {
            return; // we don't touch a database that already has auctions
        }
        User sara = userRepository.findByUsername("sara").orElseThrow();
        User omar = userRepository.findByUsername("omar").orElseThrow();
        User layla = userRepository.findByUsername("layla").orElseThrow();
        User youssef = userRepository.findByUsername("youssef").orElseThrow();
        Instant now = Instant.now();

        // 1. SCHEDULED English, opens tomorrow
        Auction scheduled = new Auction(
                "Rolex Submariner watch",
                "1998 model, serviced last year, box and papers included.",
                AuctionCategory.COLLECTIBLES, AuctionType.ENGLISH,
                new BigDecimal("5000.00"), new BigDecimal("100.00"),
                now.plus(1, ChronoUnit.DAYS), now.plus(3, ChronoUnit.DAYS), sara);
        scheduled.setCreatedAt(now.minus(6, ChronoUnit.HOURS));
        auctionRepository.save(scheduled);

        // 2. OPEN English, three bids, layla currently in front
        Auction laptop = new Auction(
                "MacBook Pro 16-inch",
                "M3 Pro, 36GB memory, 1TB storage. Bought last year, still under warranty.",
                AuctionCategory.ELECTRONICS, AuctionType.ENGLISH,
                new BigDecimal("1500.00"), new BigDecimal("50.00"),
                now.minus(4, ChronoUnit.HOURS), now.plus(1, ChronoUnit.DAYS), sara);
        laptop.setStatus(AuctionStatus.OPEN);
        laptop.setCreatedAt(now.minus(5, ChronoUnit.HOURS));
        auctionRepository.save(laptop);
        saveBid(laptop, layla, "1500.00", now.minus(3, ChronoUnit.HOURS));
        saveBid(laptop, youssef, "1550.00", now.minus(2, ChronoUnit.HOURS));
        saveBid(laptop, layla, "1600.00", now.minus(45, ChronoUnit.MINUTES));
        laptop.setCurrentHighestBid(new BigDecimal("1600.00"));

        // 3. OPEN English, layla has been outbid
        Auction bicycle = new Auction(
                "Trek mountain bike",
                "Medium frame, hydraulic disc brakes, recently serviced.",
                AuctionCategory.SPORTS, AuctionType.ENGLISH,
                new BigDecimal("600.00"), new BigDecimal("25.00"),
                now.minus(6, ChronoUnit.HOURS), now.plus(2, ChronoUnit.DAYS), omar);
        bicycle.setStatus(AuctionStatus.OPEN);
        bicycle.setCreatedAt(now.minus(7, ChronoUnit.HOURS));
        auctionRepository.save(bicycle);
        saveBid(bicycle, youssef, "600.00", now.minus(5, ChronoUnit.HOURS));
        saveBid(bicycle, layla, "625.00", now.minus(4, ChronoUnit.HOURS));
        saveBid(bicycle, youssef, "650.00", now.minus(90, ChronoUnit.MINUTES));
        bicycle.setCurrentHighestBid(new BigDecimal("650.00"));

        // 4. OPEN sealed, two hidden bids
        Auction books = new Auction(
                "The Lord of the Rings, first edition set",
                "Three volumes in the original dust jackets. Very good condition.",
                AuctionCategory.BOOKS, AuctionType.SEALED_BID,
                new BigDecimal("300.00"), null,
                now.minus(2, ChronoUnit.HOURS), now.plus(2, ChronoUnit.DAYS), omar);
        books.setStatus(AuctionStatus.OPEN);
        books.setCreatedAt(now.minus(3, ChronoUnit.HOURS));
        auctionRepository.save(books);
        saveBid(books, layla, "350.00", now.minus(80, ChronoUnit.MINUTES));
        saveBid(books, youssef, "400.00", now.minus(40, ChronoUnit.MINUTES));

        // 5. CLOSED English, won by layla
        Auction desk = new Auction(
                "Oak writing desk",
                "Late 19th century, restored last winter. Three drawers, no damage.",
                AuctionCategory.ART, AuctionType.ENGLISH,
                new BigDecimal("400.00"), new BigDecimal("25.00"),
                now.minus(3, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), sara);
        desk.setStatus(AuctionStatus.CLOSED);
        desk.setCreatedAt(now.minus(4, ChronoUnit.DAYS));
        auctionRepository.save(desk);
        saveBid(desk, youssef, "400.00", now.minus(2, ChronoUnit.DAYS));
        Bid deskWinner = saveBid(desk, layla, "450.00", now.minus(36, ChronoUnit.HOURS));
        desk.setCurrentHighestBid(new BigDecimal("450.00"));
        auctionResultRepository.save(new AuctionResult(
                desk, layla, deskWinner, deskWinner.getAmount(), now.minus(1, ChronoUnit.DAYS)));

        // 6. CLOSED sealed, won by layla. The final price is well above the
        //    starting price, which is what makes the "auctions I won" screen
        //    worth looking at: a sealed auction never publishes a running total,
        //    so the amount paid can only come from the result.
        Auction camera = new Auction(
                "Leica M6 film camera",
                "35mm rangefinder with a 50mm lens. Fully working, light meter accurate.",
                AuctionCategory.COLLECTIBLES, AuctionType.SEALED_BID,
                new BigDecimal("900.00"), null,
                now.minus(5, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS), omar);
        camera.setStatus(AuctionStatus.CLOSED);
        camera.setCreatedAt(now.minus(6, ChronoUnit.DAYS));
        auctionRepository.save(camera);
        saveBid(camera, youssef, "950.00", now.minus(4, ChronoUnit.DAYS));
        Bid cameraWinner = saveBid(camera, layla, "1100.00", now.minus(3, ChronoUnit.DAYS));
        auctionResultRepository.save(new AuctionResult(
                camera, layla, cameraWinner, cameraWinner.getAmount(), now.minus(2, ChronoUnit.DAYS)));

        // 7. CANCELLED before it opened
        Auction scooter = new Auction(
                "Electric scooter",
                "Listed by mistake, the item is no longer for sale.",
                AuctionCategory.VEHICLES, AuctionType.ENGLISH,
                new BigDecimal("800.00"), new BigDecimal("20.00"),
                now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS), sara);
        scooter.setStatus(AuctionStatus.CANCELLED);
        scooter.setCreatedAt(now.minus(2, ChronoUnit.DAYS));
        auctionRepository.save(scooter);

        log.info("Seeded 7 demo auctions (1 scheduled, 3 open, 2 closed, 1 cancelled)");
    }

    private Bid saveBid(Auction auction, User bidder, String amount, Instant at) {
        Bid bid = new Bid(new BigDecimal(amount), auction, bidder);
        bid.setCreatedAt(at);
        return bidRepository.save(bid);
    }
}
