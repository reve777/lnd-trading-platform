package com.lightning.trading.dto;

public record LndNodeInfoDto(
        String alias,
        String identityPubkey,
        int numActiveChannels,
        int numPeers,
        long blockHeight,
        boolean syncedToChain,
        String mode,
        long channelBalanceSat,
        String version
) {}
