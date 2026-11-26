"""Local stand-in for the MoMo sandbox, for end-to-end checks only.

It implements the three calls VESTRA makes (create, query) or receives (IPN) with MoMo's documented
raw-signature formats, and verifies VESTRA's request signatures, so a signing bug fails loudly here.
"""
import hashlib
import hmac
import json
import random
import sys
import time
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer

# MoMo's public sandbox sample keys (github.com/momo-wallet/payment); never real credentials.
ACCESS_KEY = "F8BBA842ECF85"
SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz"
PARTNER = "MOMO"
ORDERS = {}
LOG = []


def sign(raw):
    return hmac.new(SECRET_KEY.encode(), raw.encode(), hashlib.sha256).hexdigest()


def send_ipn(order, result_code, trans_id):
    ipn = {
        "partnerCode": PARTNER, "orderId": order["orderId"], "requestId": order["requestId"],
        "amount": order["amount"], "orderInfo": order["orderInfo"], "orderType": "momo_wallet",
        "transId": trans_id, "resultCode": result_code,
        "message": "Successful." if result_code == 0 else "Transaction denied by user.",
        "payType": "qr", "responseTime": int(time.time() * 1000), "extraData": "",
    }
    raw = ("accessKey={a}&amount={amount}&extraData={extraData}&message={message}&orderId={orderId}"
           "&orderInfo={orderInfo}&orderType={orderType}&partnerCode={partnerCode}&payType={payType}"
           "&requestId={requestId}&responseTime={responseTime}&resultCode={resultCode}&transId={transId}"
           ).format(a=ACCESS_KEY, **ipn)
    ipn["signature"] = sign(raw)
    req = urllib.request.Request(order["ipnUrl"], data=json.dumps(ipn).encode(),
                                 headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=15) as resp:
        LOG.append(f"IPN {order['orderId']} resultCode={result_code} -> HTTP {resp.status}")
        return resp.status


class Handler(BaseHTTPRequestHandler):
    def _json(self, status, body):
        data = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        if self.path == "/v2/gateway/api/create":
            req = json.loads(body)
            raw = ("accessKey={a}&amount={amount}&extraData={extraData}&ipnUrl={ipnUrl}&orderId={orderId}"
                   "&orderInfo={orderInfo}&partnerCode={partnerCode}&redirectUrl={redirectUrl}"
                   "&requestId={requestId}&requestType={requestType}").format(a=ACCESS_KEY, **req)
            if sign(raw) != req["signature"]:
                LOG.append("CREATE bad signature")
                return self._json(200, {"resultCode": 13, "message": "Merchant authentication failed"})
            req["status"] = 1000
            ORDERS[req["orderId"]] = req
            LOG.append(f"CREATE {req['orderId']} amount={req['amount']} signature OK")
            return self._json(200, {"partnerCode": PARTNER, "requestId": req["requestId"], "orderId": req["orderId"],
                                    "amount": req["amount"], "responseTime": int(time.time() * 1000),
                                    "message": "Thành công", "resultCode": 0,
                                    "payUrl": f"http://localhost:9090/pay?o={urllib.parse.quote(req['orderId'])}"})
        if self.path == "/v2/gateway/api/query":
            req = json.loads(body)
            raw = "accessKey={a}&orderId={orderId}&partnerCode={partnerCode}&requestId={requestId}".format(
                a=ACCESS_KEY, **req)
            ok = sign(raw) == req["signature"]
            order = ORDERS.get(req["orderId"])
            LOG.append(f"QUERY {req['orderId']} signature {'OK' if ok else 'BAD'}")
            if not order:
                return self._json(200, {"orderId": req["orderId"], "resultCode": 42, "message": "Not found"})
            return self._json(200, {"partnerCode": PARTNER, "orderId": order["orderId"], "requestId": req["requestId"],
                                    "amount": order["amount"], "transId": order.get("transId"),
                                    "resultCode": order["status"], "message": "", "payType": "qr",
                                    "responseTime": int(time.time() * 1000)})
        if self.path == "/pay/decide":
            form = urllib.parse.parse_qs(body.decode())
            order = ORDERS[form["o"][0]]
            code = 0 if form["action"][0] == "pay" else 1006
            trans_id = random.randint(4_000_000_000, 4_999_999_999)
            order["status"], order["transId"] = code, trans_id
            send_ipn(order, code, trans_id)
            if form.get("dup"):
                send_ipn(order, code, trans_id)  # deliver the same IPN twice
            # MoMo appends its own parameters, including its own orderId, to the merchant redirectUrl.
            extra = urllib.parse.urlencode({"partnerCode": PARTNER, "orderId": order["orderId"],
                                            "resultCode": code, "transId": trans_id})
            self.send_response(302)
            self.send_header("Location", order["redirectUrl"] + "&" + extra)
            self.end_headers()
            return
        self._json(404, {})

    def do_GET(self):
        url = urllib.parse.urlparse(self.path)
        if url.path == "/log":
            return self._json(200, LOG)
        if url.path == "/pay":
            o = urllib.parse.parse_qs(url.query)["o"][0]
            order = ORDERS[o]
            html = f"""<!doctype html><meta charset="utf-8"><title>MoMo sandbox (giả lập)</title>
<body style="font-family:sans-serif;max-width:420px;margin:40px auto">
<h2>MoMo sandbox (giả lập cho E2E)</h2><p>Đơn {o}<br>Số tiền: {order['amount']:,} đ</p>
<form method="post" action="/pay/decide"><input type="hidden" name="o" value="{o}">
<input type="hidden" name="dup" value="1">
<button name="action" value="pay" id="pay">Xác nhận thanh toán</button>
<button name="action" value="reject" id="reject">Từ chối</button></form></body>"""
            data = html.encode()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return
        self._json(404, {})

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    HTTPServer(("127.0.0.1", int(sys.argv[1]) if len(sys.argv) > 1 else 9090), Handler).serve_forever()
