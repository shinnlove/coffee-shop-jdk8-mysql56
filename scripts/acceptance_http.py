#!/usr/bin/env python3
"""Assert real HTTP behavior and persist evidence for restart checks; no third-party modules."""
import argparse
import base64
import json
import uuid
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, ProxyHandler, build_opener


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('create', 'verify'))
    parser.add_argument('--base-url', default='http://localhost:8080')
    parser.add_argument('--state', required=True)
    args = parser.parse_args()
    opener = build_opener(ProxyHandler({}))

    def request(method, path, body=None, key=None, owner=False):
        headers = {}
        data = None
        if body is not None:
            headers['Content-Type'] = 'application/json'
            data = json.dumps(body).encode('utf-8')
        if key:
            headers['Idempotency-Key'] = key
        if owner:
            headers['Authorization'] = 'Basic ' + base64.b64encode(b'owner:coffee-demo-2026').decode('ascii')
        try:
            response = opener.open(Request(args.base_url.rstrip('/') + path, data, headers, method=method), timeout=20)
        except HTTPError as error:
            response = error
        with response:
            return response.status, json.load(response), dict(response.headers.items())

    def stock():
        status, menu, _ = request('GET', '/api/v1/menu')
        assert status == 200, menu
        return {item['id']: item['availableStock'] for item in menu['items']}

    def error(method, path, status, code, body=None, key=None):
        actual, response, _ = request(method, path, body, key)
        assert actual == status and response['code'] == code, (actual, response)

    state_path = Path(args.state)
    if args.mode == 'create':
        run_id = uuid.uuid4().hex
        original_stock = stock()
        assert original_stock['cappuccino'] >= 2 and original_stock['croissant'] >= 1, 'Use a fresh acceptance database; this test consumes inventory.'
        cart = {'customerName': 'Acceptance', 'items': [{'itemId': 'cappuccino', 'quantity': 2}, {'itemId': 'croissant', 'quantity': 1}]}
        create_key, pay_key = 'accept-create-' + run_id, 'accept-pay-' + run_id
        status, order, _ = request('POST', '/api/v1/orders', cart, create_key)
        assert status == 201 and order['status'] == 'PENDING_PAYMENT', order
        assert (order['subtotalMinor'], order['discountMinor'], order['totalMinor']) == (1250, 100, 1150)
        assert stock() == original_stock
        order_path = '/api/v1/orders/' + order['id']
        error('GET', order_path + '/receipt', 409, 'ORDER_NOT_PAID')
        status, declined, _ = request('POST', order_path + '/payments/mock', {'outcome': 'DECLINE'}, 'accept-decline-' + run_id)
        assert status == 200 and declined['outcome'] == 'DECLINED' and declined['order']['status'] == 'PENDING_PAYMENT'
        assert stock() == original_stock
        status, payment, _ = request('POST', order_path + '/payments/mock', {'outcome': 'APPROVE'}, pay_key)
        assert status == 200 and payment['order']['status'] == 'PAID' and payment['order']['paidAt']
        status, receipt, _ = request('GET', order_path + '/receipt')
        assert status == 200 and receipt['totalMinor'] == 1150
        expected_stock = dict(original_stock)
        expected_stock['cappuccino'] -= 2
        expected_stock['croissant'] -= 1
        assert stock() == expected_stock
        error('GET', '/api/v1/owner/orders', 401, 'UNAUTHORIZED')
        changed_cart = dict(cart, customerName='Different content')
        error('POST', '/api/v1/orders', 409, 'IDEMPOTENCY_KEY_REUSED', changed_cart, create_key)
        invalid_cart = {'customerName': 'Acceptance', 'items': [{'itemId': 'unknown-item', 'quantity': 1}]}
        error('POST', '/api/v1/orders', 422, 'INVALID_ITEM', invalid_cart, 'accept-invalid-' + run_id)
        state = dict(cart=cart, create_key=create_key, pay_key=pay_key, order=order, payment=payment, receipt=receipt, stock=expected_stock)
        state_path.parent.mkdir(parents=True, exist_ok=True)
        state_path.write_text(json.dumps(state, indent=2), encoding='utf-8')
    else:
        state = json.loads(state_path.read_text(encoding='utf-8'))

    # Replays deliberately preserve original snapshots, including the pending create response.
    order_path = '/api/v1/orders/' + state['order']['id']
    status, replay, headers = request('POST', '/api/v1/orders', state['cart'], state['create_key'])
    assert status == 201 and replay == state['order']
    assert next(value for key, value in headers.items() if key.lower() == 'idempotency-replayed') == 'true'
    status, replay, _ = request('POST', order_path + '/payments/mock', {'outcome': 'APPROVE'}, state['pay_key'])
    assert status == 200 and replay == state['payment']
    status, new_key_payment, _ = request('POST', order_path + '/payments/mock', {'outcome': 'APPROVE'}, 'accept-retry-' + uuid.uuid4().hex)
    assert status == 200 and new_key_payment == state['payment']
    status, current, _ = request('GET', order_path)
    assert status == 200 and current == state['payment']['order']
    status, receipt, _ = request('GET', order_path + '/receipt')
    assert status == 200 and receipt == state['receipt']
    assert stock() == state['stock'], 'Restart or payment retry changed inventory'
    status, queue, _ = request('GET', '/api/v1/owner/orders?status=PAID', owner=True)
    assert status == 200 and any(order['id'] == state['order']['id'] for order in queue['orders'])
    print('PASS: HTTP ' + args.mode + '; idempotency, payment, receipt, inventory and owner authorization')


if __name__ == '__main__':
    main()
