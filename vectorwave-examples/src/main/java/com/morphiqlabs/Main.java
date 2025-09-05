package com.morphiqlabs;

import java.util.Scanner;

/**
 * Main entry point for VectorWave interactive demos.
 * Provides a menu system to explore different features and capabilities.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    VectorWave Interactive Demo Menu    ");
        System.out.println("========================================\n");
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            printMenu();
            System.out.print("Enter your choice (0 to exit): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "0" -> {
                    System.out.println("\nThank you for exploring VectorWave!");
                    scanner.close();
                    System.exit(0);
                }
                case "1" -> runDemo("com.morphiqlabs.demo.MODWTDemo");
                case "2" -> runDemo("com.morphiqlabs.demo.SWTDemo");
                case "3" -> runDemo("com.morphiqlabs.demo.cwt.CWTDemo");
                case "4" -> runDemo("com.morphiqlabs.demo.FinancialDemo");
                case "5" -> runDemo("com.morphiqlabs.demo.StreamingDenoiserDemo");
                case "6" -> runDemo("com.morphiqlabs.demo.BatchProcessingDemo");
                case "7" -> runDemo("com.morphiqlabs.demo.ParallelDenoisingDemo");
                case "8" -> runDemo("com.morphiqlabs.demo.WaveletSelectionGuideDemo");
                case "9" -> runDemo("com.morphiqlabs.demo.MemoryEfficiencyDemo");
                case "10" -> runDemo("com.morphiqlabs.demo.LiveTradingSimulation");
                case "11" -> runDemo("com.morphiqlabs.demo.cwt.FinancialWaveletsDemo");
                case "12" -> runDemo("com.morphiqlabs.demo.ScalarVsVectorDemo");
                case "13" -> runDemo("com.morphiqlabs.demo.StructuredConcurrencyDemo");
                default -> System.out.println("\nInvalid choice. Please try again.\n");
            }
            
            if (!choice.equals("0")) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }
    
    private static void printMenu() {
        System.out.println("Core Transforms:");
        System.out.println("  1. MODWT Demo - Maximal Overlap DWT for any signal length");
        System.out.println("  2. SWT Demo - Stationary Wavelet Transform");
        System.out.println("  3. CWT Demo - Continuous Wavelet Transform");
        System.out.println();
        System.out.println("Applications:");
        System.out.println("  4. Financial Analysis - Market analysis with wavelets");
        System.out.println("  5. Streaming Denoiser - Real-time signal denoising");
        System.out.println("  6. Batch Processing - SIMD-optimized batch transforms");
        System.out.println("  7. Parallel Denoising - Multi-threaded signal processing");
        System.out.println();
        System.out.println("Guides:");
        System.out.println("  8. Wavelet Selection Guide - Choose the right wavelet");
        System.out.println("  9. Memory Efficiency - Memory pooling and optimization");
        System.out.println("  10. Live Trading Simulation - Interactive market simulation");
        System.out.println("  11. Financial Wavelets - Specialized financial wavelets");
        System.out.println("  12. Performance Comparison - Scalar vs Vector operations");
        System.out.println("  13. Structured Concurrency - Guaranteed resource management");
        System.out.println();
    }
    
    private static void runDemo(String className) {
        try {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Running: " + className.substring(className.lastIndexOf('.') + 1));
            System.out.println("=".repeat(50) + "\n");
            
            Class<?> demoClass = Class.forName(className);
            var mainMethod = demoClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{});
            
        } catch (ClassNotFoundException e) {
            System.err.println("Demo not found: " + className);
        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}