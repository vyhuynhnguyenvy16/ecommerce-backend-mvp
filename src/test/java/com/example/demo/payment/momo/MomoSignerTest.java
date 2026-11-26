package com.example.demo.payment.momo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Expected signatures were computed independently of this code with Python's hmac module:
 * hmac.new(secret, raw.encode(), hashlib.sha256).hexdigest(), using the raw-string formats from the MoMo docs.
 */
class MomoSignerTest {

    static final String ACCESS_KEY = "F8BBA842ECF85";
    static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

    private final MomoSigner signer = new MomoSigner(ACCESS_KEY, SECRET_KEY);

    @Test
    void createSignatureMatchesIndependentlyComputedValue() {
        String raw = signer.createRawSignature("MOMO", 150000, "",
                "https://api.example.com/api/payments/momo/ipn", "VESTRA-42-1764150000000", "VESTRA order #42",
                "https://shop.example.com/checkout/momo-return?orderId=42", "req-0001", "captureWallet");

        assertThat(raw).isEqualTo("accessKey=F8BBA842ECF85&amount=150000&extraData="
                + "&ipnUrl=https://api.example.com/api/payments/momo/ipn&orderId=VESTRA-42-1764150000000"
                + "&orderInfo=VESTRA order #42&partnerCode=MOMO"
                + "&redirectUrl=https://shop.example.com/checkout/momo-return?orderId=42"
                + "&requestId=req-0001&requestType=captureWallet");
        assertThat(signer.sign(raw)).isEqualTo("fce7e6db152c38bc2b6c7c58466f203dde05aed1e3d0338f03947d9ba9cfe375");
    }

    @Test
    void ipnSignatureMatchesIndependentlyComputedValue() {
        MomoIpnRequest ipn = sampleIpn("afc2be0f188b0e9e157f5f49462158f3a0c2d631f475c757854eb2c978502878");

        assertThat(signer.verify(signer.ipnRawSignature(ipn), ipn.signature())).isTrue();
    }

    @Test
    void ipnVerificationRejectsTamperedAmount() {
        MomoIpnRequest original = sampleIpn("afc2be0f188b0e9e157f5f49462158f3a0c2d631f475c757854eb2c978502878");
        MomoIpnRequest tampered = new MomoIpnRequest(original.partnerCode(), original.orderId(), original.requestId(),
                1_000L, original.orderInfo(), original.orderType(), original.transId(), original.resultCode(),
                original.message(), original.payType(), original.responseTime(), original.extraData(),
                original.signature());

        assertThat(signer.verify(signer.ipnRawSignature(tampered), tampered.signature())).isFalse();
    }

    @Test
    void ipnVerificationRejectsMissingSignature() {
        MomoIpnRequest ipn = sampleIpn(null);

        assertThat(signer.verify(signer.ipnRawSignature(ipn), ipn.signature())).isFalse();
    }

    @Test
    void querySignatureMatchesIndependentlyComputedValue() {
        String raw = signer.queryRawSignature("MOMO", "VESTRA-42-1764150000000", "req-0002");

        assertThat(signer.sign(raw)).isEqualTo("524ab93cfce11bc82ddf44eb4936f9d3f2c85b7087a2bf255f24b6cb7c013db6");
    }

    static MomoIpnRequest sampleIpn(String signature) {
        return new MomoIpnRequest("MOMO", "VESTRA-42-1764150000000", "req-0001", 150000L, "VESTRA order #42",
                "momo_wallet", 4088878653L, 0, "Successful.", "qr", 1764150060000L, "", signature);
    }
}
