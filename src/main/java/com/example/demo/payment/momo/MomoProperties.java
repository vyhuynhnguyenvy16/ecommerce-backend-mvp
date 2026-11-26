package com.example.demo.payment.momo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MoMo integration settings. All values come from environment variables (see .env.example);
 * no key is hard-coded. Endpoints and field rules follow https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/
 */
@ConfigurationProperties(prefix = "momo")
public record MomoProperties(
        String endpoint,
        String partnerCode,
        String accessKey,
        String secretKey,
        String ipnUrl,
        String redirectUrl,
        String lang) {

    public boolean isConfigured() {
        return notBlank(endpoint) && notBlank(partnerCode) && notBlank(accessKey) && notBlank(secretKey)
                && notBlank(ipnUrl) && notBlank(redirectUrl);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
