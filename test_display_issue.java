import com.morphiqlabs.wavelet.api.*;
import java.util.List;

public class test_display_issue {
    public static void main(String[] args) {
        System.out.println("Demonstrating the display issue...\n");
        
        List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
        
        System.out.println("Different ways to display wavelets:");
        System.out.println("==========================================");
        
        for (WaveletName wavelet : symlets.subList(9, 14)) { // SYM11-SYM15
            System.out.println("Wavelet: " + wavelet.name());
            System.out.println("  enum.name():         " + wavelet.name());          // "SYM11" - what issue reports seeing
            System.out.println("  enum.toString():     " + wavelet.toString());      // "sym11" - code
            System.out.println("  enum.getCode():      " + wavelet.getCode());       // "sym11" - code
            System.out.println("  enum.getDescription(): " + wavelet.getDescription()); // "Symlet 11" - proper display name
            System.out.println();
        }
        
        System.out.println("The issue occurs when consuming applications use:");
        System.out.println("  wavelet.name() instead of wavelet.getDescription()");
        System.out.println("  This results in seeing 'SYM11' instead of 'Symlet 11'");
        System.out.println();
        
        System.out.println("Solution options:");
        System.out.println("1. Consumer applications should use getDescription() for display");
        System.out.println("2. OR VectorWave could make toString() return description instead of code");
        System.out.println("3. OR VectorWave could provide a getDisplayName() method that clearly indicates purpose");
    }
}