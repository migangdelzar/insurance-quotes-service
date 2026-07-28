package com.clara.insurancequotes.quote.adapter.out.persistence;

import com.clara.insurancequotes.pricing.api.type.CoverageType;
import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataQuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {

    Optional<Quote> findByIdAndUserId(UUID id, UUID userId);

    long countByStatus(QuoteStatus status);

    long countByStatusAndUserId(QuoteStatus status, UUID userId);

    long countByCoverageType(CoverageType coverageType);

    long countByCoverageTypeAndUserId(CoverageType coverageType, UUID userId);

    long countByUserId(UUID userId);

    long countByMonthlyPremiumIsNotNull();

    long countByMonthlyPremiumIsNotNullAndUserId(UUID userId);

    @Query("select coalesce(sum(q.monthlyPremium), 0) from Quote q")
    BigDecimal sumMonthlyPremium();

    @Query("select coalesce(sum(q.monthlyPremium), 0) from Quote q where q.userId = :userId")
    BigDecimal sumMonthlyPremiumForUser(@Param("userId") UUID userId);

    @Query("select coalesce(avg(q.monthlyPremium), 0) from Quote q")
    BigDecimal averageMonthlyPremium();

    @Query("select coalesce(avg(q.monthlyPremium), 0) from Quote q where q.userId = :userId")
    BigDecimal averageMonthlyPremiumForUser(@Param("userId") UUID userId);

    @Query("select q.createdAt, q.updatedAt, q.status from Quote q "
            + "where (q.createdAt >= :trendStart and q.createdAt <= :now) "
            + "or (q.updatedAt >= :trendStart and q.updatedAt <= :now)")
    List<Object[]> findTrendRows(@Param("trendStart") Instant trendStart, @Param("now") Instant now);

    @Query("select q.createdAt, q.updatedAt, q.status from Quote q "
            + "where ((q.createdAt >= :trendStart and q.createdAt <= :now) "
            + "or (q.updatedAt >= :trendStart and q.updatedAt <= :now)) and q.userId = :userId")
    List<Object[]> findTrendRowsForUser(
            @Param("trendStart") Instant trendStart, @Param("now") Instant now, @Param("userId") UUID userId);

    @Query("select q.id, q.userId from Quote q where q.status = :status and q.createdAt < :cutoff")
    List<Object[]> findStaleDraftRows(@Param("status") QuoteStatus status, @Param("cutoff") Instant cutoff);

    default List<Object[]> findStaleDraftRows(Instant cutoff) {
        return findStaleDraftRows(QuoteStatus.DRAFT, cutoff);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Quote q set q.status = :status, q.updatedAt = :now where q.id in :ids")
    int markExpired(@Param("status") QuoteStatus status, @Param("ids") List<UUID> ids, @Param("now") Instant now);

    default int markExpired(List<UUID> ids, Instant now) {
        return markExpired(QuoteStatus.EXPIRED, ids, now);
    }
}
