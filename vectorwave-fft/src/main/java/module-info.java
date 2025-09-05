module com.morphiqlabs.vectorwave.fft {
    // Export FFT API only to core to avoid leaking internals to public surface
    exports com.morphiqlabs.wavelet.fft to com.morphiqlabs.vectorwave.core;
}
