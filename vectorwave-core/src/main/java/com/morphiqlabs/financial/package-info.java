/**
 * Financial analysis module for VectorWave.
 * 
 * <p>This package provides financial time series analysis capabilities using both
 * traditional statistical methods and wavelet-based approaches. It is designed
 * for quantitative finance applications including risk analysis, volatility
 * estimation, and regime detection.</p>
 * 
 * <h2>Key Components</h2>
 * 
 * <h2>Traditional Financial Analysis</h2>
 * <ul>
 *   <li>{@link com.morphiqlabs.financial.FinancialAnalyzer} - Configurable financial
 *       metrics including crash asymmetry, volatility analysis, and regime detection</li>
 *   <li>{@link com.morphiqlabs.financial.FinancialAnalysisConfig} - Builder-pattern
 *       configuration for analysis thresholds and parameters</li>
 * </ul>
 * 
 * <h2>Wavelet-Based Financial Analysis</h2>
 * <ul>
 *   <li>{@link com.morphiqlabs.financial.FinancialWaveletAnalyzer} - Wavelet-enhanced
 *       Sharpe ratio calculations with multi-scale decomposition</li>
 *   <li>{@link com.morphiqlabs.financial.FinancialConfig} - Configuration for
 *       risk-free rates and other financial parameters</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h2>Traditional Analysis</h2>
 * <pre>{@code
 * // Create analyzer with custom thresholds
 * FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
 *     .crashAsymmetryThreshold(0.8)
 *     .volatilityHighThreshold(2.5)
 *     .windowSize(252)  // 1 year of trading days
 *     .build();
 *     
 * FinancialAnalyzer analyzer = new FinancialAnalyzer(config);
 * 
 * // Analyze price series
 * double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
 * if (analyzer.isCrashRisk(asymmetry)) {
 *     System.out.println("Warning: High crash risk detected");
 * }
 * }</pre>
 * 
 * <h2>Wavelet-Based Sharpe Ratio</h2>
 * <pre>{@code
 * // Configure risk-free rate (required - no default)
 * FinancialConfig config = new FinancialConfig(0.045);  // 4.5% annual
 *     
 * FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
 * 
 * // Calculate enhanced Sharpe ratio
 * double waveletSharpe = analyzer.calculateWaveletSharpeRatio(returns);
 * double standardSharpe = analyzer.calculateSharpeRatio(returns);
 * 
 * // Wavelet version provides better noise filtering
 * System.out.printf("Standard Sharpe: %.3f, Wavelet Sharpe: %.3f%n",
 *     standardSharpe, waveletSharpe);
 * }</pre>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Configurable thresholds for all metrics</li>
 *   <li>Multi-scale wavelet decomposition for enhanced analysis</li>
 *   <li>Crash risk detection using higher-moment analysis</li>
 *   <li>Regime shift detection</li>
 *   <li>Volatility classification (low/normal/high)</li>
 *   <li>Anomaly detection using statistical methods</li>
 * </ul>
 * 
 * <h2>Power-of-Two Requirements</h2>
 * <p>All wavelet-based methods require input arrays with power-of-two length
 * (e.g., 256, 512, 1024). Standard financial calculations do not have this
 * restriction. If your data doesn't meet this requirement, you'll need to
 * pad or truncate accordingly.</p>
 * 
 * <h2>Thread Safety</h2>
 * <p>All classes in this package are thread-safe for read operations. The
 * analyzer instances can be shared across threads for concurrent analysis
 * of different time series.</p>
 */
package com.morphiqlabs.financial;
