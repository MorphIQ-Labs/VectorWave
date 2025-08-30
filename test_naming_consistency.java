import com.morphiqlabs.wavelet.api.*;
import java.util.List;

public class test_naming_consistency {
    public static void main(String[] args) {
        System.out.println("Testing wavelet naming consistency...\n");
        
        // Check for any inconsistencies in Symlet naming
        System.out.println("Checking Symlet wavelets for naming consistency:");
        List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
        
        for (WaveletName wavelet : symlets) {
            String enumName = wavelet.name();
            String code = wavelet.getCode();
            String description = wavelet.getDescription();
            
            // Extract the number from enum name (e.g., SYM12 -> 12)
            String number = enumName.substring(3);
            
            // Check if code matches expected pattern
            String expectedCode = "sym" + number;
            String expectedDescription = "Symlet " + number;
            
            if (!code.equals(expectedCode)) {
                System.out.println("  INCONSISTENCY: " + enumName + " has code '" + code + "' but expected '" + expectedCode + "'");
            }
            
            if (!description.equals(expectedDescription)) {
                System.out.println("  INCONSISTENCY: " + enumName + " has description '" + description + "' but expected '" + expectedDescription + "'");
            }
            
            System.out.println("  " + enumName + " -> code: " + code + ", desc: " + description);
        }
        
        System.out.println("\nChecking for any gaps in Symlet sequence:");
        for (int i = 2; i <= 20; i++) {
            WaveletName expected;
            try {
                expected = WaveletName.valueOf("SYM" + i);
                if (!WaveletRegistry.hasWavelet(expected)) {
                    System.out.println("  MISSING: SYM" + i + " is defined but not registered");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("  GAP: SYM" + i + " is not defined in enum");
            }
        }
        
        System.out.println("\nChecking Daubechies wavelets for comparison:");
        List<WaveletName> daubechies = WaveletRegistry.getDaubechiesWavelets();
        for (WaveletName wavelet : daubechies.subList(0, Math.min(5, daubechies.size()))) {
            String enumName = wavelet.name();
            String code = wavelet.getCode();
            String description = wavelet.getDescription();
            System.out.println("  " + enumName + " -> code: " + code + ", desc: " + description);
        }
    }
}