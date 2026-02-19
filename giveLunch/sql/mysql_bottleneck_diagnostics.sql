-- Run with a MySQL account that can read performance_schema/sys.
-- Use this during nGrinder load windows and compare timestamps.

-- 1) Current lock waits (who blocks whom)
SELECT
    -- 대기중인 트랜잭션
    r.trx_id AS waiting_trx_id,
    r.trx_mysql_thread_id AS waiting_thread_id,
    r.trx_query AS waiting_query,
    -- 점유중인 트랜잭션
    b.trx_id AS blocking_trx_id,
    b.trx_mysql_thread_id AS blocking_thread_id,
    b.trx_query AS blocking_query
FROM information_schema.innodb_lock_waits w
JOIN information_schema.innodb_trx b ON b.trx_id = w.blocking_trx_id
JOIN information_schema.innodb_trx r ON r.trx_id = w.requesting_trx_id;

-- 2) Top SQL by total execution time(sql,실행횟수,걸린 누적합(s),평균(ms),최대값(ms))
SELECT
    DIGEST_TEXT,
    COUNT_STAR,
    ROUND(SUM_TIMER_WAIT / 1000000000000, 2) AS total_sec,
    ROUND(AVG_TIMER_WAIT / 1000000000, 2) AS avg_ms,
    ROUND(MAX_TIMER_WAIT / 1000000000, 2) AS max_ms
FROM performance_schema.events_statements_summary_by_digest
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 20;

-- 3) Top SQL by average execution time
SELECT
    DIGEST_TEXT,
    COUNT_STAR,
    ROUND(AVG_TIMER_WAIT / 1000000000, 2) AS avg_ms,
    ROUND(MAX_TIMER_WAIT / 1000000000, 2) AS max_ms
FROM performance_schema.events_statements_summary_by_digest
WHERE COUNT_STAR > 0
ORDER BY AVG_TIMER_WAIT DESC
LIMIT 20;

-- 4) Global waits and contention hotspots(대기 이벤트, 실행 횟수, 누적합)
SELECT
    EVENT_NAME,
    COUNT_STAR,
    ROUND(SUM_TIMER_WAIT / 1000000000000, 2) AS total_sec
FROM performance_schema.events_waits_summary_global_by_event_name
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 20;
