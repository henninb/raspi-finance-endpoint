# PostgreSQL Idle-in-Transaction Issue

## Date
2026-03-10

## Problem

During a routine performance audit of the `debian-dockerserver` VM (192.168.10.10),
five PostgreSQL connections from `raspi-finance-endpoint` were found stuck in
`idle in transaction` state, with the longest held open for over 17 minutes.

```
 pid  |        state        | wait_event |  idle_duration  | query
------+---------------------+------------+-----------------+-------
 7518 | idle in transaction | ClientRead | 00:17:26        | SELECT 1
 7541 | idle in transaction | ClientRead | 00:06:27        | SELECT 1
 7544 | idle in transaction | ClientRead | 00:05:38        | SELECT 1
 7547 | idle in transaction | ClientRead | 00:04:31        | SELECT 1
 7556 | idle in transaction | ClientRead | 00:00:34        | SET SESSION search_path TO 'public'
```

## Root Cause

Two settings in `src/main/resources/application-prod.yml` combine to produce this behaviour:

```yaml
# HikariCP — connections are placed in manual-commit mode
spring:
  datasource:
    hikari:
      auto-commit: false
      connection-test-query: SELECT 1   # keepalive probe

# Hibernate — told that the pool already disabled auto-commit
  jpa:
    properties:
      hibernate:
        connection.provider_disables_autocommit: true
```

With `auto-commit: false`, every connection starts in an implicit transaction.
HikariCP's keepalive probe (`SELECT 1`) fires inside that open transaction context.
If the application does not subsequently commit or roll back (e.g. after an exception
swallowed by a `@Transactional` boundary), PostgreSQL keeps the connection in
`idle in transaction` indefinitely.

**This configuration is intentional** — `auto-commit: false` combined with
`provider_disables_autocommit: true` is a documented Hibernate performance
optimisation that reduces unnecessary transaction boundary checks. It should not
be changed without load-testing the impact on batch write throughput.

## Impact

- Idle-in-transaction connections hold row-level locks, blocking `VACUUM` from
  reclaiming dead tuples (table bloat over time).
- At `maximum-pool-size: 20`, enough leaked connections could exhaust the pool
  and cause connection timeouts in the application.
- No data corruption or downtime was observed from this incident.

## Fix Attempted — `ALTER DATABASE` Timeout

A database-level timeout was applied:

```sql
ALTER DATABASE finance_db SET idle_in_transaction_session_timeout = '5min';
```

### Why It Did Not Work

PostgreSQL's `idle_in_transaction_session_timeout` is measured from
`pg_stat_activity.state_change` — the last time the connection *entered*
the `idle in transaction` state, not from when the transaction opened
(`xact_start`).

HikariCP sends a `SELECT 1` keepalive probe to each idle connection roughly
every 1–2 minutes (`connection-test-query: SELECT 1`). Each probe briefly
sets the state to `active`, then back to `idle in transaction`, which resets
`state_change`. The 5-minute timeout clock never reaches zero.

Observed evidence:

```
 pid  | txn_age  | idle_since  | query
------+----------+-------------+----------
 7547 | 00:12:55 | 00:01:39    | SELECT 1
 7544 | 00:14:03 | 00:01:29    | SELECT 1
 7541 | 00:14:51 | 00:00:48    | SELECT 1
```

Transactions 12–14 minutes old but PostgreSQL only sees 48 seconds of idle
time because the keepalive just fired.

### Root Cause (Revised)

This is **expected behaviour** for the current HikariCP configuration:

- `minimum-idle: 5` — HikariCP always maintains 5 warm connections
- `auto-commit: false` — every connection starts an implicit transaction
- `connection-test-query: SELECT 1` — keepalive resets the idle timer

The five `idle in transaction` connections are not leaked — HikariCP owns
and manages them. They are warm standby connections waiting to serve the
next request.

The `ALTER DATABASE` timeout is left in place as a safety net for genuinely
abandoned connections (where HikariCP has stopped probing), but it does not
eliminate the steady-state idle connections.

## Actual Remediation Options

### Option 1 — Reduce minimum idle connections (lowest risk)
```yaml
hikari:
  minimum-idle: 0   # no warm connections; slight latency on first request after idle
```
Eliminates idle-in-transaction connections entirely at the cost of a small
connection setup delay after periods of inactivity.

### Option 2 — Switch to auto-commit mode (breaks Hibernate optimisation)
```yaml
hikari:
  auto-commit: true
jpa:
  properties:
    hibernate:
      connection.provider_disables_autocommit: false  # or remove
```
Eliminates idle-in-transaction state but removes the Hibernate batching
optimisation. Requires load testing before applying to production.

### Option 3 — Accept current behaviour (chosen for now)
The connections are managed by HikariCP and pose no data-corruption risk.
The main downside is that VACUUM cannot reclaim dead tuples while these
five transactions are open. Monitor table bloat via:

```sql
SELECT relname, n_dead_tup, last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

## Long-term Recommendation

Audit `@Transactional` service methods for exception paths that may exit without
an explicit commit or rollback. A missing `rollbackFor` on checked exceptions is
a common cause:

```java
// Risk: checked exceptions do NOT trigger rollback by default
@Transactional
public void process() throws MyCheckedException { ... }

// Safe: explicitly include checked exceptions
@Transactional(rollbackFor = Exception.class)
public void process() throws MyCheckedException { ... }
```

The `leak-detection-threshold: 60000` (60 s) setting in HikariCP will log a
warning when a connection is held longer than 60 seconds, which can help catch
genuinely leaked connections in application logs.
