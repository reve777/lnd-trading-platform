package com.lightning.trading.client;

import com.lightning.trading.dto.LndNodeInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Component("mockLndClient")
public class MockLndClientImpl implements LndClient {

    private static final Logger log = LoggerFactory.getLogger(MockLndClientImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AtomicLong channelBalanceSat = new AtomicLong(50_000_000L); // 50M sats initial
    private final String nodePubkey = "02" + HexFormat.of().formatHex(generateRandomBytes(32));

    @Override
    public LndNodeInfoDto getInfo() {
        return new LndNodeInfoDto(
                "Lightning-LND-Node-Regtest",
                nodePubkey,
                14,
                8,
                850_120L,
                true,
                "MOCK_SIMULATOR",
                channelBalanceSat.get(),
                "0.18.2-beta"
        );
    }

    @Override
    public long getChannelBalanceSat() {
        return channelBalanceSat.get();
    }

    @Override
    public LndInvoiceResponse createInvoice(long amountSat, String memo) {
        byte[] preimage = generateRandomBytes(32);
        String paymentHash = sha256Hex(preimage);

        // Generate synthetic BOLT11 payment request invoice string
        String invoice = "lnbc" + amountSat + "u1p" + HexFormat.of().formatHex(generateRandomBytes(20)) +
                "pp5" + paymentHash.substring(0, 16) + "mocklninvoice" + System.currentTimeMillis();

        log.info("[LND MOCK] Created Lightning Invoice for {} sats, memo: '{}', hash: {}",
                amountSat, memo, paymentHash);

        return new LndInvoiceResponse(paymentHash, invoice, System.currentTimeMillis());
    }

    @Override
    public LndPaymentResponse sendPayment(String paymentRequest, long amountSat) {
        // Simulate real-world Lightning multi-hop route propagation and HTLC settlement latency (80 ~ 220 ms)
        long simulatedRouteLatencyMs = ThreadLocalRandom.current().nextLong(80, 220);
        try {
            Thread.sleep(simulatedRouteLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (channelBalanceSat.get() < amountSat) {
            return new LndPaymentResponse(
                    null,
                    null,
                    0,
                    0,
                    false,
                    "FAILURE_REASON_INSUFFICIENT_LOCAL_BALANCE: Outgoing channel capacity exceeded"
            );
        }

        channelBalanceSat.addAndGet(-amountSat);

        byte[] preimageBytes = generateRandomBytes(32);
        String preimageHex = HexFormat.of().formatHex(preimageBytes);
        String paymentHash = sha256Hex(preimageBytes);

        // Routing fee calculation: 1 sat base fee + 0.05% ppm fee
        long feeSat = Math.max(1L, (long) Math.ceil(amountSat * 0.0005));
        int hopCount = ThreadLocalRandom.current().nextInt(2, 5); // 2 to 4 network hops

        log.info("[LND MOCK] Lightning Payment Settled: {} sats, fee: {} sats, hops: {}, preimage: {}",
                amountSat, feeSat, hopCount, preimageHex);

        return new LndPaymentResponse(
                preimageHex,
                paymentHash,
                feeSat,
                hopCount,
                true,
                null
        );
    }

    @Override
    public boolean isMockMode() {
        return true;
    }

    private static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
