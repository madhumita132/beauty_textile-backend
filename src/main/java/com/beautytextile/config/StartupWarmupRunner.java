package com.beautytextile.config;

import com.beautytextile.repository.BillingRepository;
import com.beautytextile.repository.CategoryRepository;
import com.beautytextile.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Warms up key repositories after startup so first real user request is faster.
 */
@Component
@Order(100)
public class StartupWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupWarmupRunner.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BillingRepository billingRepository;

    public StartupWarmupRunner(ProductRepository productRepository,
                               CategoryRepository categoryRepository,
                               BillingRepository billingRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.billingRepository = billingRepository;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        long started = System.currentTimeMillis();
        try {
            // Small count queries initialize JPA metadata and open JDBC pool connections early.
            productRepository.count();
            categoryRepository.count();
            billingRepository.count();
            log.info("Startup warmup completed in {} ms", System.currentTimeMillis() - started);
        } catch (Exception ex) {
            log.warn("Startup warmup skipped: {}", ex.getMessage());
        }
    }
}
