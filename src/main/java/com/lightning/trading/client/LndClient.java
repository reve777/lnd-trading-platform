package com.lightning.trading.client;

import com.lightning.trading.dto.LndNodeInfoDto;

public interface LndClient {

    /**
     * Retrieves Lightning Node information.
     */
    LndNodeInfoDto getInfo();

    /**
     * Retrieves the channel balance in Satoshis.
     */
    long getChannelBalanceSat();

    /**
     * Issues a BOLT11 payment request invoice.
     */
    LndInvoiceResponse createInvoice(long amountSat, String memo);

    /**
     * Sends a lightning payment using a BOLT11 invoice.
     */
    LndPaymentResponse sendPayment(String paymentRequest, long amountSat);

    /**
     * Indicates whether this client is operating in simulation/mock mode.
     */
    boolean isMockMode();
}
