#!/bin/bash
# SPARK26 Lab — Load Generator
# Sends a steady stream of requests to order-service to generate observable traffic
# Usage: ./load-gen.sh [requests-per-second] [duration-seconds]
#
# Default: 2 req/s for 5 minutes
# This will trigger all three bugs within a few minutes.

BASE_URL="http://localhost:8080"
RPS=${1:-2}
DURATION=${2:-300}
SLEEP=$(awk "BEGIN { printf \"%.3f\", 1/$RPS }")

echo "Starting load generator: ${RPS} req/s for ${DURATION}s"
echo "Press Ctrl+C to stop"
echo ""

END=$((SECONDS + DURATION))
COUNT=0

while [ $SECONDS -lt $END ]; do
  COUNT=$((COUNT + 1))

  # 50% — list all orders (triggers BUG #2: latency spike)
  curl -s "$BASE_URL/orders" > /dev/null &

  # 20% — get a valid order
  curl -s "$BASE_URL/orders/ORD-001" > /dev/null &

  # 15% — get the broken order (triggers BUG #1: silent exception)
  curl -s "$BASE_URL/orders/ORD-ERR" > /dev/null &

  # 15% — create an order for PROD-200 (triggers BUG #3: inventory timeout)
  curl -s -X POST "$BASE_URL/orders" \
    -H "Content-Type: application/json" \
    -d '{"customerId":"CUST-TEST","productId":"PROD-200","quantity":1,"unitPrice":"19.99"}' \
    > /dev/null &

  if [ $((COUNT % 10)) -eq 0 ]; then
    echo "[$(date +%H:%M:%S)] Sent $COUNT request cycles..."
  fi

  sleep "$SLEEP"
done

echo "Load generation complete. $COUNT cycles sent."
