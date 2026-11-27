#!/usr/bin/env bash
set -euo pipefail

base="${BASE_URL:-http://localhost:8080}"

# Each run represents a new purchase and uses new idempotency keys.
# Retries of the same request must reuse its original key and body.
run_id="$(date +%s)-${RANDOM}"

# Print a separate heading, request address and divider for each API call.
print_step() {
  printf '\n%s\n' '================================================================'
  printf 'Interface curl step %s: %s\n' "$1" "$2"
  printf 'Request: %s\n' "$3"
  printf '%s\n' '----------------------------------------------------------------'
}

# Pretty-print JSON while preserving Unicode; no jq dependency is required.
pretty_json() {
  python3 -c 'import json,sys; print(json.dumps(json.load(sys.stdin), ensure_ascii=False, indent=2))'
}

# Step 1: Fetch categories, menu items, prices, stock and combo offers.
print_step 1 'Get menu: categories, items, stock and combo offers' "GET $base/api/v1/menu"
printf 'Response:\n'
curl -fsS "$base/api/v1/menu" | pretty_json

# Step 2: Create a pending order for two cappuccinos and one croissant.
# Order creation checks stock but does not reserve or deduct it.
print_step 2 'Create order: 2 cappuccinos + 1 croissant' "POST $base/api/v1/orders"
create_order_body='{"customerName":"Alex","items":[{"itemId":"cappuccino","quantity":2},{"itemId":"croissant","quantity":1}]}'
printf 'Request body:\n'
printf '%s\n' "$create_order_body" | pretty_json

order="$(curl -fsS \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: demo-create-$run_id" \
  -d "$create_order_body" \
  "$base/api/v1/orders")"

printf '\nResponse:\n'
printf '%s\n' "$order" | pretty_json

# Extract the generated order ID for the payment and receipt requests.
order_id="$(printf '%s' "$order" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')"
printf '\nOrder ID: %s\n' "$order_id"

# Step 3: Approve the mock payment. A successful payment marks the order
# as PAID, deducts stock and creates a receipt in one database transaction.
print_step 3 'Mock payment: APPROVE' "POST $base/api/v1/orders/$order_id/payments/mock"
mock_payment_body='{"outcome":"APPROVE"}'
printf 'Request body:\n'
printf '%s\n' "$mock_payment_body" | pretty_json

printf '\nResponse:\n'
curl -fsS \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: demo-pay-$run_id" \
  -d "$mock_payment_body" \
  "$base/api/v1/orders/$order_id/payments/mock" | pretty_json

# Step 4: Retrieve the receipt, which is available only after payment.
print_step 4 'Get receipt: order number, payment time, items and totals' "GET $base/api/v1/orders/$order_id/receipt"
printf 'Response:\n'
curl -fsS "$base/api/v1/orders/$order_id/receipt" | pretty_json

# Step 5: Fetch the paid-order queue using local demo owner credentials.
# The queue may also contain orders paid during previous demo runs.
print_step 5 'Owner order queue: list paid orders' "GET $base/api/v1/owner/orders?status=PAID"
printf 'Authentication: owner HTTP Basic\n\nResponse:\n'
curl -fsS -u 'owner:coffee-demo-2026' \
  "$base/api/v1/owner/orders?status=PAID" | pretty_json

printf '\n%s\n' '================================================================'
printf 'Demo flow completed. Order ID: %s\n' "$order_id"
