package com.veely.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "spring.jpa.properties.hibernate.generate_statistics", havingValue = "true")
public class HibernateStatisticsConfig {
    
    private final SessionFactory sessionFactory;
    
    public HibernateStatisticsConfig(EntityManagerFactory emf) {
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.sessionFactory.getStatistics().setStatisticsEnabled(true);
    }
    
    @Scheduled(fixedDelay = 60000) // Ogni minuto
    public void logStatistics() {
        Statistics stats = sessionFactory.getStatistics();
        
        if (stats.getQueryExecutionCount() > 0) {
            log.info("=== Hibernate Statistics ===");
            log.info("Query eseguite: {}", stats.getQueryExecutionCount());
            log.info("Tempo max query: {} ms", stats.getQueryExecutionMaxTime());
            log.info("Entity caricate: {}", stats.getEntityLoadCount());
            log.info("Entity fetchate: {}", stats.getEntityFetchCount());
            log.info("Collection caricate: {}", stats.getCollectionLoadCount());
            log.info("Collection fetchate: {}", stats.getCollectionFetchCount());
            
            // Log query lente
            String slowestQuery = stats.getQueryExecutionMaxTimeQueryString();
            if (slowestQuery != null) {
                log.warn("Query più lenta: {} ({} ms)", 
                    slowestQuery, stats.getQueryExecutionMaxTime());
            }
            
            // Reset statistiche dopo il log
            stats.clear();
        }
    }
}