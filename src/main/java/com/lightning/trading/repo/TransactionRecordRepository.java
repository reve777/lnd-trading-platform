package com.lightning.trading.repo;

import com.lightning.trading.entity.TransactionRecord;
import com.lightning.trading.util.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    Optional<TransactionRecord> findByTxUid(String txUid);

    Optional<TransactionRecord> findByUuidv7Full(String uuidv7Full);

    List<TransactionRecord> findAllByOrderByCreatedAtDesc();

    List<TransactionRecord> findTop100ByOrderByCreatedAtDesc();

    List<TransactionRecord> findByTxTypeOrderByCreatedAtDesc(TransactionType txType);

    @Query("SELECT r.txType as txType, COUNT(r) as count, AVG(r.executionTimeMs) as avgLatency, " +
           "MIN(r.executionTimeMs) as minLatency, MAX(r.executionTimeMs) as maxLatency " +
           "FROM TransactionRecord r WHERE r.status = 'SUCCESS' GROUP BY r.txType")
    List<TxBenchmarkStats> getBenchmarkStatistics();

    interface TxBenchmarkStats {
        TransactionType getTxType();
        Long getCount();
        Double getAvgLatency();
        Long getMinLatency();
        Long getMaxLatency();
    }
}
