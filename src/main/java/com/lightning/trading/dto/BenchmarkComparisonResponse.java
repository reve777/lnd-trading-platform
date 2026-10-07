package com.lightning.trading.dto;

public record BenchmarkComparisonResponse(
        TradeResponse standardTrade,
        TradeResponse lndTrade,
        long latencyDifferenceMs,
        double latencyRatio,
        String analysisSummary
) {}
