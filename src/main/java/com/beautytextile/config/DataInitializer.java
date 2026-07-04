package com.beautytextile.config;

import com.beautytextile.model.*;
import com.beautytextile.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Seeds default admin user, categories, GST settings, billing user, and sample reviews on first run. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminRepo;
    private final CategoryRepository categoryRepo;
    private final AppSettingsRepository appSettingsRepo;
    private final ReviewRepository reviewRepo;
    private final OrderRepository orderRepo;
    private final ProductVariantSizeRepository variantSizeRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(AdminUserRepository adminRepo,
                           CategoryRepository categoryRepo,
                           AppSettingsRepository appSettingsRepo,
                           ReviewRepository reviewRepo,
                           OrderRepository orderRepo,
                           ProductVariantSizeRepository variantSizeRepo,
                           PasswordEncoder encoder) {
        this.adminRepo = adminRepo;
        this.categoryRepo = categoryRepo;
        this.appSettingsRepo = appSettingsRepo;
        this.reviewRepo = reviewRepo;
        this.orderRepo = orderRepo;
        this.variantSizeRepo = variantSizeRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        seedAdminUsers();
        seedCategories();
        seedAppSettings();
        seedReviews();
        backfillFulfillmentStatus();
        backfillBarcodes();
    }

    // ── Admin Users ───────────────────────────────────────────────────────────

    private void seedAdminUsers() {
        if (adminRepo.findByUsername("admin").isEmpty()) {
            adminRepo.save(AdminUser.builder()
                    .username("admin")
                    .password(encoder.encode("admin123"))
                    .role("ADMIN")
                    .build());
            System.out.println("[DataInitializer] Created user: admin / admin123");
        }
        // BILLING user for POS-only access
        if (adminRepo.findByUsername("bill").isEmpty()) {
            adminRepo.save(AdminUser.builder()
                    .username("bill")
                    .password(encoder.encode("bill123"))
                    .role("BILLING")
                    .build());
            System.out.println("[DataInitializer] Created user: bill / bill123");
        }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    private void seedCategories() {
        Category sarees = upsert("Sarees", null, "/images/categories/saree/saree.svg");
        Category women  = upsert("Women",  null, "/images/categories/women.svg");
        Category men    = upsert("Men",    null, "/images/categories/mens/mens.svg");
        Category kids   = upsert("Kids",   null, "/images/categories/kids/kids.svg");

        upsert("Daily Wear",        sarees, "/images/categories/saree/daily-wear.svg");
        upsert("Party Wear Saree",  sarees, "/images/categories/saree/party-wear.svg");
        upsert("Cotton Saree",      sarees, "/images/categories/saree/cotton.svg");
        upsert("KanjiPattu Saree",  sarees, "/images/categories/saree/kanji-pattu.svg");
        upsert("Pattu Saree",       sarees, "/images/categories/saree/pattu-silk.svg");

        upsert("Kurthi", women, "/images/categories/kurthi/kurthi.svg");
        upsert("Tops",   women, "/images/categories/kurthi/tops.svg");
        upsert("Chudi",  women, "/images/categories/kurthi/chudi.svg");
        upsert("Gown",   women, "/images/categories/kurthi/gown.svg");
        upsert("Legin",  women, "/images/categories/kurthi/legin.svg");
        upsert("Shawl",  women, "/images/categories/kurthi/shawl.svg");

        upsert("Shirt",       men, "/images/categories/mens/shirt.svg");
        upsert("Pant",        men, "/images/categories/mens/pant.svg");
        upsert("Ethnic Wear", men, "/images/categories/mens/ethnic-wear.svg");

        upsert("Boys Collection",  kids, "/images/categories/kids/boys.svg");
        upsert("Girls Collection", kids, "/images/categories/kids/girls.svg");
    }

    // ── App Settings (GST) ────────────────────────────────────────────────────

    private void seedAppSettings() {
        if (appSettingsRepo.existsById(1L)) return;
        appSettingsRepo.save(AppSettings.builder()
                .gstEnabled(false)
                .gstPercentage(0)
                .build());
        System.out.println("[DataInitializer] Created default app_settings (GST disabled)");
    }

    // ── Sample Reviews ────────────────────────────────────────────────────────

    private void seedReviews() {
        if (reviewRepo.count() > 0) return;

        Object[][] data = {
            // productId, name, rating, comment, status, reply, daysAgo
            {1L, "Priya S.",    5, "Beautiful fabric quality, very happy with the purchase!",      "APPROVED", "Thank you Priya! We're delighted you loved it 🌸", 1},
            {1L, "Ramesh K.",   5, "Fast delivery and the saree looks exactly like the photo.",     "APPROVED", null,                                              4},
            {2L, "Sunita M.",   5, "Excellent stitching and colors are vibrant. Highly recommend!", "APPROVED", "Thank you for your kind words!",                  7},
            {2L, "Kavitha R.",  4, "Good product but slightly different shade than expected.",      "APPROVED", null,                                              10},
            {3L, "Arun T.",     5, "Perfect fit, my wife loved it. Will order again.",              "APPROVED", null,                                              13},
            {3L, "Meena D.",    5, "Best quality kurti I have bought online. Worth the price.",     "PENDING",  null,                                              2},
            {1L, "Vijay P.",    4, "Material is soft and comfortable. Great for daily wear.",       "APPROVED", "Thank you! We put great care into our fabrics.",  16},
            {2L, "Lakshmi N.",  5, "Packaging was neat and delivery was quick. Satisfied!",        "APPROVED", null,                                              20},
            {1L, "Deepa M.",    3, "Average quality but price is reasonable.",                     "REJECTED", null,                                              25},
            {3L, "Suresh B.",   5, "Absolutely stunning saree! Got many compliments at the party.","PENDING",  null,                                              3},
        };

        for (Object[] d : data) {
            Review r = new Review();
            r.setProductId((Long) d[0]);
            r.setCustomerName((String) d[1]);
            r.setRating((int) d[2]);
            r.setReviewComment((String) d[3]);
            r.setStatus((String) d[4]);
            r.setAdminReply((String) d[5]);
            r.setCreatedAt(LocalDateTime.now().minusDays((int) d[6]));
            reviewRepo.save(r);
        }
        System.out.println("[DataInitializer] Seeded 10 sample reviews");
    }

    // ── Backfill fulfillment_status for existing orders ───────────────────────

    private void backfillFulfillmentStatus() {
        orderRepo.findAll().forEach(order -> {
            if (order.getFulfillmentStatus() == null || order.getFulfillmentStatus().isBlank()) {
                boolean paid = PaymentStatus.PAID.equals(order.getPaymentStatus());
                order.setFulfillmentStatus(paid ? "CONFIRMED" : "PENDING");
                orderRepo.save(order);
            }
        });
    }

    // ── Backfill barcodes for existing variant sizes ──────────────────────────

    private void backfillBarcodes() {
        variantSizeRepo.findAll().forEach(size -> {
            if (size.getBarcode() == null || size.getBarcode().isBlank()) {
                size.setBarcode(String.format("BT%07d", size.getId()));
                variantSizeRepo.save(size);
            }
        });
    }

    /** Create if absent, update parent + imagePath if already exists. */
    private Category upsert(String name, Category parent, String imagePath) {
        Category c = categoryRepo.findByNameIgnoreCase(name).orElseGet(Category::new);
        c.setName(name);
        c.setParent(parent);
        c.setImagePath(imagePath);
        return categoryRepo.save(c);
    }
}

