import com.morphiqlabs.wavelet.api.*;
import java.util.List;

public class test_issue {
    public static void main(String[] args) {
        System.out.println("Testing the actual issue from the bug report...\n");
        
        // Test the method mentioned in the issue
        List<WaveletName> cwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.CWT);
        
        System.out.println("Wavelets returned for CWT transform:");
        for (WaveletName wavelet : cwtWavelets) {
            System.out.println("  " + wavelet + " (" + wavelet.getCode() + ") - " + wavelet.getDescription());
        }
        
        System.out.println("\nChecking if any Symlet wavelets are incorrectly included in CWT:");
        boolean foundSymletInCWT = false;
        for (WaveletName wavelet : cwtWavelets) {
            if (wavelet.name().startsWith("SYM")) {
                System.out.println("  PROBLEM: " + wavelet + " is included in CWT but is ORTHOGONAL type");
                foundSymletInCWT = true;
            }
        }
        
        if (!foundSymletInCWT) {
            System.out.println("  No Symlet wavelets found in CWT list (this is correct)");
        }
        
        System.out.println("\nTesting MODWT transform (should include Symlets):");
        List<WaveletName> modwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.MODWT);
        System.out.println("Found " + modwtWavelets.size() + " wavelets for MODWT");
        
        System.out.println("\nSymlet wavelets in MODWT (should show proper display names):");
        for (WaveletName wavelet : modwtWavelets) {
            if (wavelet.name().startsWith("SYM")) {
                System.out.println("  " + wavelet + " (" + wavelet.getCode() + ") - " + wavelet.getDescription());
            }
        }
    }
}