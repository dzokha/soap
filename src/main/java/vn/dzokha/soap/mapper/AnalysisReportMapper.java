package vn.dzokha.soap.mapper;

import vn.dzokha.soap.dto.response.AnalysisReportDTO;
import vn.dzokha.soap.domain.job.AnalysisResult;
import vn.dzokha.soap.engine.qc.core.*;
import vn.dzokha.soap.engine.qc.modules.*;
import vn.dzokha.soap.engine.qc.modules.adapter.AdapterContent;
import vn.dzokha.soap.engine.qc.modules.kmer.KmerContent;

import java.util.Arrays;
import java.util.Random;

public class AnalysisReportMapper {

    public static AnalysisReportDTO toDTO(AnalysisResult result) {
        AnalysisReportDTO dto = new AnalysisReportDTO();
        dto.setFileName(result.getFileName());
        dto.setStatus(result.getStatus());

        // ====================================================================
        // 1. GẮP DỮ LIỆU THẬT TỪ CÁC MODULES
        // ====================================================================
        if (result.getModules() != null) {
            for (QCModule module : result.getModules()) {

                // 1. Adapter Content & Per Base Quality (Bạn đã làm xong)
                if (module instanceof AdapterContent adapter) {
                    dto.setAdapterData(adapter.getAdapterContentData());
                } 
                else if (module instanceof PerBaseQualityScores qualityModule) {
                    dto.setPerBaseQualityData(qualityModule.getBoxPlotData());
                }

                // 2. Per Sequence Quality Scores
                else if (module instanceof PerSequenceQualityScores seqQual) {
                    /* HƯỚNG DẪN: Lấy mảng đếm điểm Quality từ class PerSequenceQualityScores
                     * Mở class đó ra, viết hàm getCounts() trả về mảng double[] hoặc long[]
                     * * Ví dụ:
                     * double[] counts = seqQual.getCounts();
                     * for (int i = 0; i < counts.length; i++) {
                     * if (counts[i] > 0) {
                     * dto.getPerSeqQualityData().add(new AnalysisReportDTO.PerSeqQuality(i, (long)counts[i]));
                     * }
                     * }
                     */
                }

                // 3. Per Base Sequence Content
                else if (module instanceof PerBaseSequenceContent baseContent) {
                    /* HƯỚNG DẪN: Lấy các mảng %A, %C, %G, %T và trục X (Labels)
                     * * Ví dụ:
                     * String[] labels = baseContent.getLabels();
                     * double[] aCount = baseContent.getA();
                     * double[] cCount = baseContent.getC(); ...
                     * * for (int i = 0; i < labels.length; i++) {
                     * dto.getPerBaseContentData().add(new AnalysisReportDTO.PerBaseContent(
                     * labels[i], aCount[i], cCount[i], gCount[i], tCount[i]
                     * ));
                     * }
                     */
                }

                // 4. Per Sequence GC Content
                else if (module instanceof PerSequenceGCContent gcContent) {
                    /* HƯỚNG DẪN: Lấy phân phối GC thực tế và GC lý thuyết
                     * * double[] actual = gcContent.getActualData();
                     * double[] theoretical = gcContent.getTheoreticalData();
                     * for (int i = 0; i <= 100; i++) {
                     * dto.getGcContentData().add(new AnalysisReportDTO.GcContent(i, (long)actual[i], (long)theoretical[i]));
                     * }
                     */
                }

                // 5. Per Base N Content
                else if (module instanceof NContent nContent) {
                    /* HƯỚNG DẪN: 
                     * String[] labels = nContent.getLabels();
                     * double[] nCounts = nContent.getNContent();
                     * for(int i=0; i<labels.length; i++) {
                     * dto.getNContentData().add(new AnalysisReportDTO.NContent(labels[i], nCounts[i]));
                     * }
                     */
                }

                // 6. Sequence Length Distribution
                else if (module instanceof SequenceLengthDistribution lenDist) {
                    /* HƯỚNG DẪN:
                     * int[] lengths = lenDist.getLengths();
                     * long[] counts = lenDist.getCounts();
                     * for(int i=0; i<lengths.length; i++) {
                     * dto.getLengthDistributionData().add(new AnalysisReportDTO.LengthDistribution(lengths[i], counts[i]));
                     * }
                     */
                }

                // 7. Sequence Duplication Levels
                else if (module instanceof DuplicationLevel dupLevel) {
                    // Logic tương tự: Lặp qua dữ liệu Dedup và Total rồi add vào dto.getDuplicationLevelsData()
                }

                // 8. Overrepresented Sequences
                else if (module instanceof OverRepresentedSeqs overRep) {
                    /* HƯỚNG DẪN:
                     * List<OverrepresentedSeq> list = overRep.getOverrepresentedSequences();
                     * (Chỉ cần gán thẳng nếu list đã đúng định dạng, hoặc lặp để map sang DTO)
                     */
                }
            }
        }

        // ====================================================================
        // 2. ĐIỀN DỮ LIỆU GIẢ (MOCK DATA) NẾU MODULE CHƯA CODE XONG
        // ====================================================================
        fillMockDataIfEmpty(dto);

        return dto;
    }

    /**
     * Hàm này kiểm tra: Nếu list nào đang rỗng (size = 0) tức là bạn chưa kịp bỏ comment 
     * và viết code gắp dữ liệu thật ở phía trên, nó sẽ tạm bơm dữ liệu giả vào để Web vẫn vẽ được.
     */
    private static void fillMockDataIfEmpty(AnalysisReportDTO dto) {
        Random rand = new Random();

        if (dto.getPerSeqQualityData().isEmpty()) {
            for (int i = 10; i <= 40; i++) {
                long count = (long) (Math.exp(-(Math.pow(i - 36, 2)) / 10) * 150000);
                if (count > 10) dto.getPerSeqQualityData().add(new AnalysisReportDTO.PerSeqQuality(i, count));
            }
        }

        if (dto.getPerBaseContentData().isEmpty()) {
            for (int i = 1; i <= 150; i++) {
                dto.getPerBaseContentData().add(new AnalysisReportDTO.PerBaseContent(
                    String.valueOf(i), 20 + rand.nextDouble() * 10, 20 + rand.nextDouble() * 10, 20 + rand.nextDouble() * 10, 20 + rand.nextDouble() * 10
                ));
            }
        }

        if (dto.getGcContentData().isEmpty()) {
            for (int i = 0; i <= 100; i++) {
                long actual = (long) (Math.exp(-(Math.pow(i - 50, 2)) / 50) * 30000);
                long theoretical = (long) (Math.exp(-(Math.pow(i - 48, 2)) / 50) * 25000);
                dto.getGcContentData().add(new AnalysisReportDTO.GcContent(i, actual, theoretical));
            }
        }

        if (dto.getNContentData().isEmpty()) {
            for (int i = 1; i <= 150; i++) {
                dto.getNContentData().add(new AnalysisReportDTO.NContent(String.valueOf(i), rand.nextDouble() * 0.5));
            }
        }

        if (dto.getLengthDistributionData().isEmpty()) {
            dto.getLengthDistributionData().add(new AnalysisReportDTO.LengthDistribution(149, 0));
            dto.getLengthDistributionData().add(new AnalysisReportDTO.LengthDistribution(150, 400000));
            dto.getLengthDistributionData().add(new AnalysisReportDTO.LengthDistribution(151, 0));
        }

        if (dto.getDuplicationLevelsData().isEmpty()) {
            String[] levels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", ">10", ">50", ">100", ">500", ">1k", ">5k", ">10k"};
            for (String level : levels) {
                dto.getDuplicationLevelsData().add(new AnalysisReportDTO.DuplicationLevel(
                    level, 
                    Math.max(0, 60 - Arrays.asList(levels).indexOf(level) * 10 + rand.nextDouble() * 5),
                    Math.max(0, 20 - Arrays.asList(levels).indexOf(level) * 3 + rand.nextDouble() * 2)
                ));
            }
        }

        if (dto.getOverrepresentedData().isEmpty()) {
            dto.getOverrepresentedData().add(new AnalysisReportDTO.OverrepresentedSeq("CCCGCCGCCGCCAGCTTCAGCTTTCCCCCATAGGGGGTGGCTGACTGGCAC", 3726, 0.9357, "No Hit"));
            dto.getOverrepresentedData().add(new AnalysisReportDTO.OverrepresentedSeq("ACTCACACCAGCGTACTAGTACACGAGTACAGCAAGCCACAGCGCCTACA", 2689, 0.6753, "No Hit"));
        }
    }
}