package com.morphiqlabs.financial;

/**
 * Configuration class for financial analysis parameters.
 * 
 * <p>This class requires users to explicitly specify financial parameters
 * such as the risk-free rate, ensuring that analysis is performed with
 * appropriate market-specific values rather than potentially outdated defaults.</p>
 * 
 * <p><strong>Risk-Free Rate Guidelines:</strong></p>
 * <ul>
 *   <li>US Treasury Bills (3-month) for short-term analysis</li>
 *   <li>US Treasury Notes (10-year) for long-term analysis</li>
 *   <li>Central bank rates for the relevant currency</li>
 *   <li>High-grade corporate bonds for corporate finance</li>
 * </ul>
 * 
 * <p>Example rates across different periods:</p>
 * <ul>
 *   <li>2020-2021: Near 0% (pandemic era)</li>
 *   <li>2023-2024: 4-5% (rate hike cycle)</li>
 *   <li>Historical average (1962-2023): ~4.4%</li>
 * </ul>
 */
public class FinancialConfig {
    
    private final double riskFreeRate;
    
    /**
     * Creates a financial configuration with specified risk-free rate.
     * 
     * @param riskFreeRate the risk-free rate as an annual decimal (e.g., 0.045 for 4.5%)
     * @throws IllegalArgumentException if riskFreeRate is negative
     */
    public FinancialConfig(double riskFreeRate) {
        if (riskFreeRate < 0) {
            throw new IllegalArgumentException("Risk-free rate cannot be negative: " + riskFreeRate);
        }
        this.riskFreeRate = riskFreeRate;
    }
    
    /**
     * Returns the configured risk-free rate.
     * 
     * @return the risk-free rate as an annual decimal
     */
    public double getRiskFreeRate() {
        return riskFreeRate;
    }
    
    @Override
    public String toString() {
        return String.format("FinancialConfig{riskFreeRate=%.4f}", riskFreeRate);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FinancialConfig)) return false;
        FinancialConfig other = (FinancialConfig) obj;
        return Double.compare(riskFreeRate, other.riskFreeRate) == 0;
    }
    
    @Override
    public int hashCode() {
        return Double.hashCode(riskFreeRate);
    }
}