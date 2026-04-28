package vn.dzokha.soap.engine.trimming;

/**
 * Class này tương đương với phần GetOptions() trong file Perl của Trim Galore.
 * Nó nhận cấu hình từ Frontend (React) truyền xuống.
 */
public class TrimConfig {
    private int qualityCutoff = 20; // -q 20
    private String adapterSequence; // -a (VD: AGATCGGAAGAGC cho Illumina)
    private int minLength = 20;     // --length 20
    private int minOverlap = 1;     // --stringency 1
    private double errorRate = 0.1; // -e 0.1 (Tỉ lệ lỗi cho phép khi so khớp)

    public TrimConfig() {}

    public int getQualityCutoff() { return qualityCutoff; }
    public void setQualityCutoff(int qualityCutoff) { this.qualityCutoff = qualityCutoff; }

    public String getAdapterSequence() { return adapterSequence; }
    public void setAdapterSequence(String adapterSequence) { this.adapterSequence = adapterSequence; }

    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }

    public int getMinOverlap() { return minOverlap; }
    public void setMinOverlap(int minOverlap) { this.minOverlap = minOverlap; }

    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
}