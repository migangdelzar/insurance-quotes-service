package com.clara.insurancequotes.quote.adapter.out.persistence;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataQuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {

    long countByStatus(QuoteStatus status);

    long countByCoverageType(CoverageType coverageType);

    long countByMonthlyPremiumIsNotNull();

    @Query("select coalesce(sum(q.monthlyPremium), 0) from Quote q")
    BigDecimal sumMonthlyPremium();

    @Query("select coalesce(avg(q.monthlyPremium), 0) from Quote q")
    BigDecimal averageMonthlyPremium();

    @Query("select q.createdAt, q.updatedAt, q.status from Quote q "
            + "where (q.createdAt >= :trendStart and q.createdAt <= :now) "
            + "or (q.updatedAt >= :trendStart and q.updatedAt <= :now)")
    List<Object[]> findTrendRows(@Param("trendStart") Instant trendStart, @Param("now") Instant now);

    @Query("select q.id from Quote q where q.status = :status and q.createdAt < :cutoff")
    List<UUID> findIdsToExpire(@Param("status") QuoteStatus status, @Param("cutoff") Instant cutoff);

    default List<UUID> findIdsToExpire(Instant cutoff) {
        return findIdsToExpire(QuoteStatus.DRAFT, cutoff);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Quote q set q.status = :status, q.updatedAt = :now where q.id in :ids")
    int markExpired(@Param("status") QuoteStatus status, @Param("ids") List<UUID> ids, @Param("now") Instant now);

    default int markExpired(List<UUID> ids, Instant now) {
        return markExpired(QuoteStatus.EXPIRED, ids, now);
    }
}
