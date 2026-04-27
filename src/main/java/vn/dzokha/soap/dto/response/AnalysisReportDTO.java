
package vn.dzokha.soap.dto.response;

import java.util.ArrayList;
import java.util.List;

public class AnalysisReportDTO {
    private String fileName;
    private String status;
    
    // ========================================================================
    // CÁC DANH SÁCH DỮ LIỆU BIỂU ĐỒ (Ánh xạ trực tiếp sang JSON cho D3.js)
    // ========================================================================
    
    private List<?> adapterData = new ArrayList<>();
    private List<?> perBaseQualityData = new ArrayList<>();
    
    private List<PerSeqQuality> perSeqQualityData = new ArrayList<>();
    private List<PerBaseContent> perBaseContentData = new ArrayList<>();
    private List<GcContent> gcContentData = new ArrayList<>();
    private List<NContent> nContentData = new ArrayList<>();
    private List<LengthDistribution> lengthDistributionData = new ArrayList<>();
    private List<DuplicationLevel> duplicationLevelsData = new ArrayList<>();
    private List<OverrepresentedSeq> overrepresentedData = new ArrayList<>();

    // ========================================================================
    // GETTERS & SETTERS CHO DTO CHÍNH
    // ========================================================================

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<?> getAdapterData() { return adapterData; }
    public void setAdapterData(List<?> adapterData) { this.adapterData = adapterData; }

    public List<?> getPerBaseQualityData() { return perBaseQualityData; }
    public void setPerBaseQualityData(List<?> perBaseQualityData) { this.perBaseQualityData = perBaseQualityData; }

    public List<PerSeqQuality> getPerSeqQualityData() { return perSeqQualityData; }
    public void setPerSeqQualityData(List<PerSeqQuality> perSeqQualityData) { this.perSeqQualityData = perSeqQualityData; }

    public List<PerBaseContent> getPerBaseContentData() { return perBaseContentData; }
    public void setPerBaseContentData(List<PerBaseContent> perBaseContentData) { this.perBaseContentData = perBaseContentData; }

    public List<GcContent> getGcContentData() { return gcContentData; }
    public void setGcContentData(List<GcContent> gcContentData) { this.gcContentData = gcContentData; }

    public List<NContent> getNContentData() { return nContentData; }
    public void setNContentData(List<NContent> nContentData) { this.nContentData = nContentData; }

    public List<LengthDistribution> getLengthDistributionData() { return lengthDistributionData; }
    public void setLengthDistributionData(List<LengthDistribution> lengthDistributionData) { this.lengthDistributionData = lengthDistributionData; }

    public List<DuplicationLevel> getDuplicationLevelsData() { return duplicationLevelsData; }
    public void setDuplicationLevelsData(List<DuplicationLevel> duplicationLevelsData) { this.duplicationLevelsData = duplicationLevelsData; }

    public List<OverrepresentedSeq> getOverrepresentedData() { return overrepresentedData; }
    public void setOverrepresentedData(List<OverrepresentedSeq> overrepresentedData) { this.overrepresentedData = overrepresentedData; }

    // ========================================================================
    // CÁC INNER CLASSES ĐẠI DIỆN CHO CẤU TRÚC JSON CỦA TỪNG LOẠI BIỂU ĐỒ
    // ========================================================================

    // 1. Per Sequence Quality Scores -> { quality: 20, count: 100 }
    public static class PerSeqQuality {
        public int quality;
        public long count;
        public PerSeqQuality(int quality, long count) { this.quality = quality; this.count = count; }
    }

    // 2. Per Base Sequence Content -> { position: "1", a: 25, c: 25, g: 25, t: 25 }
    public static class PerBaseContent {
        public String position;
        public double a, c, g, t;
        public PerBaseContent(String position, double a, double c, double g, double t) {
            this.position = position; this.a = a; this.c = c; this.g = g; this.t = t;
        }
    }

    // 3. Per Sequence GC Content -> { gcPercent: 1, actualCount: 10, theoreticalCount: 12 }
    public static class GcContent {
        public int gcPercent;
        public long actualCount;
        public long theoreticalCount;
        public GcContent(int gcPercent, long actualCount, long theoreticalCount) {
            this.gcPercent = gcPercent; this.actualCount = actualCount; this.theoreticalCount = theoreticalCount;
        }
    }

    // 4. Per Base N Content -> { position: "1", nPercent: 0.5 }
    public static class NContent {
        public String position;
        public double nPercent;
        public NContent(String position, double nPercent) {
            this.position = position; this.nPercent = nPercent;
        }
    }

    // 5. Sequence Length Distribution -> { length: 150, count: 10000 }
    public static class LengthDistribution {
        public int length;
        public long count;
        public LengthDistribution(int length, long count) {
            this.length = length; this.count = count;
        }
    }

    // 6. Sequence Duplication Levels -> { level: "1", dedupPercent: 55, totalPercent: 18 }
    public static class DuplicationLevel {
        public String level;
        public double dedupPercent;
        public double totalPercent;
        public DuplicationLevel(String level, double dedupPercent, double totalPercent) {
            this.level = level; this.dedupPercent = dedupPercent; this.totalPercent = totalPercent;
        }
    }

    // 7. Overrepresented Sequences -> { sequence: "ATCG...", count: 3726, percentage: 0.93, source: "No Hit" }
    public static class OverrepresentedSeq {
        public String sequence;
        public long count;
        public double percentage;
        public String source;
        public OverrepresentedSeq(String sequence, long count, double percentage, String source) {
            this.sequence = sequence; this.count = count; this.percentage = percentage; this.source = source;
        }
    }
}
// package vn.dzokha.soap.dto;

// import java.util.List;
// import java.util.ArrayList;

// public class AnalysisReportDTO {
//     private String fileName;
//     private String status;
    
//     // Dữ liệu cho biểu đồ Adapter Content
//     private List<?> adapterData = new ArrayList<>();
    
//     // THÊM MỚI: Dữ liệu cho biểu đồ Per Base Sequence Quality
//     private List<?> perBaseQualityData = new ArrayList<>();

//     public String getFileName() { return fileName; }
//     public void setFileName(String fileName) { this.fileName = fileName; }
//     public String getStatus() { return status; }
//     public void setStatus(String status) { this.status = status; }

//     public List<?> getAdapterData() { return adapterData; }
//     public void setAdapterData(List<?> adapterData) { this.adapterData = adapterData; }

//     // THÊM MỚI: Getter / Setter cho Quality Data
//     public List<?> getPerBaseQualityData() { return perBaseQualityData; }
//     public void setPerBaseQualityData(List<?> perBaseQualityData) { this.perBaseQualityData = perBaseQualityData; }
// }