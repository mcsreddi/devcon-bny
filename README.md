# Observable by Default
## SPARK26 Lab — Hands-on Observability with OpenTelemetry, Prometheus & Grafana

> **Session:** Wednesday October 14 · ChandraSekar R Mula · BNY SPARK26

---

## What You'll Build

You'll take a broken Spring Boot service from **zero observability** to **production-ready** in 90 minutes. The service has three real bugs hiding inside it — you can't find them without instrumentation.

By the end you will have:
- ✅ A service fully instrumented with OpenTelemetry
- ✅ Metrics flowing into Prometheus, visualized in Grafana
- ✅ Distributed traces in Jaeger showing exactly where things break
- ✅ An instrumentation pattern you can copy into your own services tomorrow

---

## Prerequisites

Install these before the session:

| Tool | Version | Check |
|------|---------|-------|
| Java | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker Desktop | Latest | `docker ps` |
| Git | Any | `git --version` |

---

## Quick Start (do this before the session!)

```bash
# 1. Clone the repo
git clone https://github.com/mcsreddi/devcon-bny
cd devcon-bny

# 2. Start the observability stack
docker compose up -d

# 3. In a new terminal — run inventory-service
cd inventory-service
mvn clean spring-boot:run

# 4. In another terminal — run order-service (the starter)
cd order-service
mvn clean spring-boot:run

# 5. Verify everything is up
curl http://localhost:8080/orders
curl http://localhost:8081/inventory/PROD-100
```

> **Windows:** in PowerShell, use `curl.exe` instead of `curl`. Run the services from a normal terminal (not Administrator), and don't click inside their windows while they run — selecting text pauses the app.

**Open these in your browser:**
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

**Repo layout:**

| Folder | What it is |
|---|---|
| `order-service/` | The starter you work in: three silent bugs, OpenTelemetry commented out |
| `order-service-solution/` | The finished version: fully instrumented, every change marked `// SOLUTION:` |
| `inventory-service/` | The downstream dependency order-service calls (not instrumented) |

---

## The Three Bugs

The service has **three hidden problems**. Your job is to find them using observability.

| # | Symptom | Where to look |
|---|---------|--------------|
| Bug 1 | `GET /orders/ORD-ERR` returns 200 OK with empty body — no error anywhere | Traces → look for missing spans or error attributes |
| Bug 2 | `GET /orders` is usually fast, sometimes takes 2–4 seconds | Grafana → p99 latency panel, then Jaeger to find the slow trace |
| Bug 3 | `POST /orders` for PROD-200 silently uses fake inventory data | Traces → look at the inventory call's span (`GET :8081/inventory/...`) |

---

## Lab Steps

All three edits are in the starter `order-service/` and are marked `LAB STEP 1/2/3` in the files.

### Step 1 — Add OTel dependency (15 min)

In `order-service/pom.xml`, delete the `<!--` and `-->` lines around the OTel starter so it reads:

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

No `<version>` tag is needed: the OpenTelemetry BOM in `<dependencyManagement>` sets it (2.3.0-alpha).

### Step 2 — Enable Prometheus metrics (5 min)

In `order-service/src/main/resources/application.yml`, uncomment the `LAB STEP 2` block under `management:` (keep the 2-space indent):

```yaml
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

The histogram setting publishes latency buckets, which the Grafana p50 / p95 / p99 panel needs.

### Step 3 — Wire the OTel exporter (5 min)

Still in `application.yml`, uncomment the `LAB STEP 3` block (`otel:` starts at column 0):

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318
  service:
    name: order-service
```

Port 4318 is the Collector's OTLP-over-HTTP receiver, which is what the Spring Boot starter uses.

Restart order-service (`Ctrl+C`, then `mvn clean spring-boot:run`). Within about 30 seconds, `order-service` appears in Jaeger's Service dropdown.

### Step 4 — Generate traffic (2 min)

From the repo root, in **Git Bash** (on Windows, plain `bash` in PowerShell starts WSL and fails):

```bash
./load-gen.sh            # 2 req/s for 5 minutes
./load-gen.sh 2 1800     # 2 req/s for 30 minutes
```

This sends a steady stream of all request types, triggering all three bugs. Leave it running.

### Step 5 — Hunt the bugs (40 min)

Open Grafana → Dashboards → **Order Service — SPARK26 Lab**. Give it a minute of traffic first.

**Find Bug #2 first** (easiest): Watch the p50 vs p99 latency panel. When p99 spikes but p50 stays flat, open Jaeger: Service `order-service`, Operation `GET /orders`, Min Duration `2s`. Find the slow trace — what's different about it?

**Find Bug #1 next**: In Jaeger, Operation `GET /orders/{orderId}`, Tags `url.path=/orders/ORD-ERR`. Why does the span show no error even though the response was empty?

**Find Bug #3 last**: In Jaeger, Operation `POST /orders`, Min Duration `1.9s`. Open a trace and click the child `GET` span — that is the call to inventory-service on port 8081. It timed out. What does the parent `POST /orders` span say happened? (Only about 30% of PROD-200 calls time out, so keep load-gen running.)

### Step 6 — Fix & verify (10 min)

The fixes are in `order-service-solution/`. Stop the starter and run the solution on the same port:

```bash
cd order-service-solution
mvn clean spring-boot:run
```

Compare the two folders (in VS Code: right-click a file → **Select for Compare**, then the matching file → **Compare with Selected**). Every change is marked `// SOLUTION:`.

Then look again in Jaeger (lookback "Last 5 minutes"):

| Bug | Jaeger search | What you see now |
|---|---|---|
| 1 | Tags `error=true` | `ORD-ERR` returns 500; the `OrderService.getOrder` span is red with the NullPointerException attached |
| 2 | Operation `GET /orders`, Min Duration `2s` | The `OrderService.listOrders` span has a `slow_path_triggered` event and `slow_path.delay_ms` |
| 3 | Tags `inventory.fallback=true` | `OrderService.createOrder` is red with the timeout. `POST /orders` stays green because the customer still got 200 CONFIRMED |

The key insight: adding `span.recordException(e)` and `span.setStatus(ERROR)` to every catch block is what makes bugs visible in traces. Auto-instrumentation shows what happened on the network; only your code can show what you decided to do about it.

---

## Solution

If you get stuck, `order-service-solution/` has the fully instrumented version with all bugs made visible and documented.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Port 8080 was already in use` | Another app holds 8080. Find it with `netstat -ano \| findstr :8080` and stop it. Run the starter and the solution one at a time |
| Every PROD-200 order takes 2s | inventory-service is paused or hung. Press Esc in its window, or restart it |
| `UnsupportedClassVersionError` | Old compiled classes. Use `mvn clean spring-boot:run` |
| Nothing in Jaeger after Step 3 | Check the dependency is uncommented and `otel:` starts at column 0, then run `docker logs otel-collector` |
| Grafana p99 panel empty | Check the Step 2 block's indentation and give it a minute of traffic |
| `mvn` or `docker` not found in the VS Code terminal | Fully close and reopen VS Code so it picks up the current PATH |

---

## Key Takeaway

> Metrics tell you *something is wrong*.  
> Traces tell you *where and why*.  
> Logs tell you *what happened*.  
>
> You need all three. OTel gives you all three with one dependency.
