package com.clara.insurancequotes.quote.adapter.out.persistence;

import com.clara.insurancequotes.quote.domain.model.Quote;
import com.clara.insurancequotes.quote.domain.model.QuoteStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataQuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {

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
