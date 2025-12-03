# Performance Monitoring Guide

This document describes the comprehensive performance monitoring system added to identify and diagnose performance bottlenecks.

## 🎯 Overview

The performance monitoring system provides:

1. **Method-level timing** - Track execution time for all service and repository methods
2. **SQL query logging** - Log all database queries with execution times
3. **Console metrics reporting** - Human-readable performance summaries every 60 seconds
4. **Correlation IDs** - Trace individual requests across all logs
5. **REST API endpoint** - Query performance metrics programmatically
6. **Zero configuration** - Works out of the box with sensible defaults

**Performance Overhead**: ~1-3% (Moderate)

## 📊 Features

### 1. Method Execution Timing (AOP-based)

**What it does:**
- Automatically tracks execution time for ALL methods in `finance.services.*` and `finance.repositories.*`
- Logs performance with correlation IDs
- Records metrics to Micrometer for analysis
- Provides different log levels based on execution time

**Log Levels:**
- `INFO`: Methods completing in <500ms
- `WARN`: Methods taking 500ms-2000ms (performance concern)
- `ERROR`: Methods taking >2000ms (critical performance issue)

**Example Log Output:**
```
[PERF] [a1b2c3d4-5678-90ef-1234-567890abcdef] SERVICE.TransactionService.insertTransaction took 1250ms (exceeds 500ms threshold)
[PERF] [a1b2c3d4-5678-90ef-1234-567890abcdef] REPOSITORY.TransactionRepository.save took 850ms (exceeds 500ms threshold)
```

**Implementation:**
- `PerformanceMonitoringAspect.kt` - AOP aspect that intercepts all service/repository methods
- Uses `@Around` advice for minimal overhead
- Publishes `method.execution.time` metrics with tags: `layer`, `class`, `method`, `status`

**Configuration:**
```yaml
performance:
  method-timing:
    enabled: true                   # Enable/disable method timing
    warn-threshold-ms: 500          # Warn threshold (default: 500ms)
    error-threshold-ms: 2000        # Error threshold (default: 2000ms)
```

### 2. SQL Query Logging with Execution Times

**What it does:**
- Logs every SQL query before execution
- Tracks execution time for each query
- Highlights slow queries (>100ms) and very slow queries (>500ms)
- Sanitizes sensitive data (passwords, tokens, secrets)
- Adds correlation IDs to SQL queries

**Log Levels:**
- `DEBUG`: All queries
- `INFO`: Queries completing in <100ms
- `WARN`: Slow queries (100ms-500ms)
- `ERROR`: Very slow queries (>500ms)

**Example Log Output:**
```
[SQL] [a1b2c3d4-5678-90ef-1234-567890abcdef] Executing: SELECT * FROM t_transaction WHERE account_name_owner = ?
[SQL] [a1b2c3d4-5678-90ef-1234-567890abcdef] Slow query completed in 350ms (threshold: 100ms)
```

**Implementation:**
- `SqlQueryLoggingInterceptor.kt` - Hibernate StatementInspector that intercepts SQL
- `SqlQueryPerformanceLogger.kt` - Tracks query completion and execution time
- Integrated with Hibernate via `HibernatePropertiesCustomizer`

**Configuration:**
```yaml
performance:
  sql-logging:
    enabled: true                        # Enable/disable SQL logging
    slow-query-threshold-ms: 100         # Slow query warning threshold
    very-slow-query-threshold-ms: 500    # Very slow query error threshold

spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true        # Enable Hibernate statistics
        use_sql_comments: true           # Add comments to SQL
        format_sql: true                 # Format SQL for readability
```

### 3. Console Metrics Reporter

**What it does:**
- Prints a comprehensive performance report to the console every 60 seconds
- Provides human-readable metrics summaries
- Shows top slowest methods, endpoints, and resource usage
- Includes visual indicators (✅ success, ❌ error, ⚠️ warning)

**Report Sections:**
1. **Method Execution Metrics** - Top 10 slowest methods per layer (service/repository)
2. **HTTP Request Metrics** - Total requests, status counts, slowest endpoints
3. **Database Metrics** - Connection pool status, circuit breaker state
4. **JVM Metrics** - Heap memory, GC pauses, thread counts
5. **System Metrics** - CPU usage (system and process)

**Example Console Output:**
```
================================================================================
📊 PERFORMANCE METRICS REPORT
================================================================================

🔧 METHOD EXECUTION METRICS:
  [service Layer]
    ✅ TransactionService.insertTransaction: count=1,234, avg=245.50ms, max=1,850.00ms
    ✅ AccountService.findAll: count=567, avg=125.30ms, max=890.00ms
  [repository Layer]
    ✅ TransactionRepository.findByAccountNameOwner: count=890, avg=85.20ms, max=450.00ms

🌐 HTTP REQUEST METRICS:
  Total Requests: 5,678
    ✅ HTTP 200: 5,234 requests
    ⚠️ HTTP 400: 123 requests
    ❌ HTTP 500: 21 requests
  Slowest Endpoints:
    POST /transactions: max=1,950.00ms, avg=450.25ms

💾 DATABASE METRICS:
  HikariCP Connection Pool:
    Active: 8
    Idle: 12
    Pending: 0
    Total: 20
  Circuit Breaker: ✅ closed

☕ JVM METRICS:
  Heap Memory: 512MB / 2,048MB (25.00%)
  GC Pauses: 45 collections, avg=12.50ms
  Active Threads: 42

💻 SYSTEM METRICS:
  CPU Usage: 35.25%
  Process CPU: 28.50%

================================================================================
```

**Implementation:**
- `ConsoleMetricsReporter.kt` - Scheduled task that reports metrics
- Uses `@Scheduled(fixedDelay = 60000)` for periodic reporting
- Queries Micrometer MeterRegistry for all metrics

**Configuration:**
```yaml
performance:
  console-reporting:
    enabled: true                  # Enable/disable console reporting
    interval-seconds: 60           # Report interval (default: 60s)
```

### 4. Correlation IDs (MDC)

**What it does:**
- Assigns a unique UUID to each HTTP request
- Propagates correlation ID through all logs (service methods, SQL queries, etc.)
- Returns correlation ID in response headers (`X-Correlation-ID`)
- Allows tracing a single request through the entire stack

**Example:**
```
Request: GET /transactions
Response Header: X-Correlation-ID: a1b2c3d4-5678-90ef-1234-567890abcdef

All logs for this request will include:
[a1b2c3d4-5678-90ef-1234-567890abcdef] ...
```

**Implementation:**
- `CorrelationIdFilter.kt` - Servlet filter that manages correlation IDs
- Uses SLF4J MDC (Mapped Diagnostic Context)
- Highest precedence to ensure it runs first

**Usage:**
- Client can send `X-Correlation-ID` header to use their own ID
- If not provided, a UUID is generated automatically
- Correlation ID is cleared after request completes (prevents memory leaks)

### 5. Performance Metrics REST API

**What it does:**
- Provides REST endpoints to query performance metrics programmatically
- Returns JSON responses with detailed performance data
- Useful for dashboards, monitoring tools, or troubleshooting

**Endpoints:**

#### GET `/performance/summary`
Returns overall performance summary with all metrics.

**Response:**
```json
{
  "methodExecutionMetrics": { ... },
  "httpRequestMetrics": { ... },
  "databaseMetrics": { ... },
  "jvmMetrics": { ... },
  "timestamp": 1701234567890
}
```

#### GET `/performance/methods`
Returns detailed method execution metrics.

**Response:**
```json
{
  "serviceLayerTop20": [
    {
      "className": "TransactionService",
      "methodName": "insertTransaction",
      "layer": "service",
      "status": "success",
      "count": 1234,
      "avgTimeMs": 245.5,
      "maxTimeMs": 1850.0,
      "totalTimeMs": 303123.0
    }
  ],
  "repositoryLayerTop20": [ ... ],
  "slowest10Overall": [ ... ],
  "mostCalled10": [ ... ]
}
```

#### GET `/performance/database`
Returns database connection pool and circuit breaker metrics.

**Response:**
```json
{
  "connectionPool": {
    "active": 8,
    "idle": 12,
    "pending": 0,
    "total": 20,
    "utilizationPercent": 40.0
  },
  "circuitBreakerState": "closed"
}
```

#### GET `/performance/http`
Returns HTTP request metrics.

**Response:**
```json
{
  "totalRequests": 5678,
  "statusCounts": {
    "200": 5234,
    "400": 123,
    "500": 21
  },
  "slowestEndpoints": [ ... ],
  "mostCalledEndpoints": [ ... ]
}
```

#### GET `/performance/jvm`
Returns JVM memory, GC, and CPU metrics.

**Response:**
```json
{
  "heapMemory": {
    "usedMB": 512.0,
    "maxMB": 2048.0,
    "usagePercent": 25.0
  },
  "garbageCollection": {
    "totalCollections": 45,
    "totalPauseTimeMs": 562.5,
    "avgPauseTimeMs": 12.5
  },
  "cpu": {
    "systemUsagePercent": 35.25,
    "processUsagePercent": 28.5
  }
}
```

**Implementation:**
- `PerformanceMetricsController.kt` - REST controller with 5 endpoints
- Queries Micrometer MeterRegistry for metrics
- Returns structured JSON responses

## 🚀 Getting Started

### 1. Build with Dependencies

The AOP dependency has been added to `build.gradle`:

```bash
./gradlew clean build
```

### 2. Run the Application

Source your environment variables and run:

```bash
source env.secrets
./run-bootrun.sh
```

Or manually:

```bash
export SPRING_PROFILES_ACTIVE=prod
./gradlew bootRun
```

### 3. Monitor Performance

**Option 1: Watch Console Output**

The console will automatically print performance reports every 60 seconds.

**Option 2: Use REST API**

```bash
# Get overall summary
curl http://localhost:8080/performance/summary

# Get method metrics
curl http://localhost:8080/performance/methods

# Get database metrics
curl http://localhost:8080/performance/database

# Get HTTP metrics
curl http://localhost:8080/performance/http

# Get JVM metrics
curl http://localhost:8080/performance/jvm
```

**Option 3: Search Logs by Correlation ID**

```bash
# Tail logs and grep for a specific correlation ID
tail -f logs/raspi-finance-endpoint.log | grep "a1b2c3d4-5678-90ef-1234-567890abcdef"

# This shows ALL operations for that single request
```

## 🔍 Troubleshooting Slow Performance

### Step 1: Identify Slow Endpoints

Look at the console metrics report or query `/performance/http`:

```
Slowest Endpoints:
  POST /transactions: max=1,950.00ms, avg=450.25ms
```

### Step 2: Find Slow Methods

Check the method execution metrics:

```
[PERF] SERVICE.TransactionService.insertTransaction took 1250ms
[PERF] REPOSITORY.TransactionRepository.save took 850ms
```

### Step 3: Identify Slow SQL Queries

Look for SQL warnings/errors:

```
[SQL] Slow query completed in 350ms (threshold: 100ms)
[SQL] VERY SLOW QUERY completed in 750ms (threshold: 500ms)
```

### Step 4: Trace a Specific Request

1. Make a request and note the correlation ID from the response header
2. Search logs for that correlation ID
3. See the complete execution path with timings

```bash
curl -v http://localhost:8080/transactions
# Response header: X-Correlation-ID: abc123...

grep "abc123" logs/raspi-finance-endpoint.log
```

### Step 5: Check Resource Utilization

Look at the database and JVM metrics:

```
💾 DATABASE METRICS:
  Active: 18/20 (90% utilization) ⚠️  <- Connection pool exhaustion

☕ JVM METRICS:
  Heap Memory: 1,800MB / 2,048MB (87.89%) ⚠️  <- Memory pressure
```

## 📈 Performance Optimization Tips

Based on monitoring data, here are common fixes:

### Problem: Slow SQL Queries (>100ms)

**Diagnosis:**
```
[SQL] Slow query completed in 350ms: SELECT * FROM t_transaction WHERE ...
```

**Solutions:**
1. Add database indexes
2. Optimize query (avoid SELECT *, use pagination)
3. Enable query result caching
4. Review N+1 query patterns

### Problem: High Connection Pool Utilization (>80%)

**Diagnosis:**
```
Connection Pool: Active: 18/20 (90%)
```

**Solutions:**
1. Increase `maximum-pool-size` in application-prod.yml
2. Reduce connection timeout
3. Check for connection leaks
4. Optimize slow queries

### Problem: Slow Service Methods (>500ms)

**Diagnosis:**
```
[PERF] SERVICE.TransactionService.insertTransaction took 1250ms
```

**Solutions:**
1. Profile method to find bottlenecks
2. Reduce database round trips (batch operations)
3. Add caching for frequently accessed data
4. Move heavy processing to background jobs

### Problem: High Memory Usage (>80%)

**Diagnosis:**
```
Heap Memory: 1,800MB / 2,048MB (87.89%)
```

**Solutions:**
1. Increase JVM heap size: `-Xmx4g`
2. Review large object allocations
3. Enable GC logging for analysis
4. Check for memory leaks

### Problem: Circuit Breaker Opening

**Diagnosis:**
```
Circuit Breaker: ❌ open
```

**Solutions:**
1. Check database connectivity
2. Review recent slow queries
3. Investigate connection pool exhaustion
4. Check application logs for errors

## ⚙️ Configuration Reference

All configuration in `application-prod.yml`:

```yaml
performance:
  # Console reporting
  console-reporting:
    enabled: true
    interval-seconds: 60

  # SQL query logging
  sql-logging:
    enabled: true
    slow-query-threshold-ms: 100
    very-slow-query-threshold-ms: 500

  # Method timing
  method-timing:
    enabled: true
    warn-threshold-ms: 500
    error-threshold-ms: 2000

# Hibernate SQL logging
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
        use_sql_comments: true
        format_sql: true
```

## 📝 Log Format

All logs now include correlation IDs:

**Console:**
```
2024-12-03 10:45:23 [http-nio-8080-exec-1] [a1b2c3d4-5678-90ef] - INFO  TransactionService - [PERF] ...
```

**File (logs/raspi-finance-endpoint.log):**
```
2024-12-03 10:45:23 [http-nio-8080-exec-1] [a1b2c3d4-5678-90ef] INFO TransactionService - [PERF] ...
```

## 🎓 Best Practices

1. **Monitor Regularly**: Review console metrics reports to establish baselines
2. **Trace Slow Requests**: Use correlation IDs to investigate slow requests end-to-end
3. **Set Alerts**: Configure alerting on error-level performance logs (>2000ms methods)
4. **Profile Production**: Use REST API endpoints to gather production performance data
5. **Optimize Iteratively**: Focus on the slowest operations first (biggest impact)
6. **Load Test**: Use performance profile to test under load and identify bottlenecks

## 🛠️ Files Created/Modified

### New Files:
- `src/main/kotlin/finance/configurations/PerformanceMonitoringAspect.kt`
- `src/main/kotlin/finance/configurations/SqlQueryLoggingInterceptor.kt`
- `src/main/kotlin/finance/configurations/ConsoleMetricsReporter.kt`
- `src/main/kotlin/finance/configurations/CorrelationIdFilter.kt`
- `src/main/kotlin/finance/configurations/PerformanceMonitoringConfiguration.kt`
- `src/main/kotlin/finance/controllers/PerformanceMetricsController.kt`
- `PERFORMANCE_MONITORING.md` (this file)

### Modified Files:
- `build.gradle` - Added `spring-boot-starter-aop` dependency
- `src/main/resources/application-prod.yml` - Added performance configuration
- `src/main/resources/logback.xml` - Added correlation ID to log pattern

## 📞 Support

If you encounter issues or have questions:
1. Check console metrics reports for obvious issues
2. Review logs with correlation ID tracing
3. Query REST API endpoints for detailed metrics
4. Check this guide for troubleshooting tips

## 🎯 Next Steps

1. **Run the application and monitor console output** to establish performance baselines
2. **Use correlation IDs** to trace slow requests through the system
3. **Query REST API endpoints** to gather detailed performance data
4. **Identify bottlenecks** using the slowest methods/queries reports
5. **Optimize** the most impactful performance issues first
6. **Iterate** until performance meets requirements
