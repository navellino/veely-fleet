package com.veely.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for document statistics displayed in the admin documents dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStatistics {
    
    private long totalCount;
    private long validCount;
    private long expiringSoonCount;
    private long expiredCount;
    private long noExpiryCount;
    
    /**
     * Get percentage of valid documents
     */
    public double getValidPercentage() {
        return totalCount > 0 ? (validCount * 100.0) / totalCount : 0.0;
    }
    
    /**
     * Get percentage of expired documents
     */
    public double getExpiredPercentage() {
        return totalCount > 0 ? (expiredCount * 100.0) / totalCount : 0.0;
    }
    
    /**
     * Get percentage of expiring soon documents
     */
    public double getExpiringSoonPercentage() {
        return totalCount > 0 ? (expiringSoonCount * 100.0) / totalCount : 0.0;
    }
    
    /**
     * Check if there are any urgent documents (expired or expiring soon)
     */
    public boolean hasUrgentDocuments() {
        return expiredCount > 0 || expiringSoonCount > 0;
    }
    
    /**
     * Get count of urgent documents
     */
    public long getUrgentCount() {
        return expiredCount + expiringSoonCount;
    }
}
