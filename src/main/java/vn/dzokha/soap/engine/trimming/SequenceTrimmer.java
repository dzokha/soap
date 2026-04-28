package vn.dzokha.soap.engine.trimming;

import vn.dzokha.soap.domain.sequence.Sequence;
import org.springframework.stereotype.Component;

/**
 * ĐÂY CHÍNH LÀ LÕI CHẤT XÁM! 
 * Thay vì gọi Cutadapt bằng Python, Java sẽ tự thao tác cắt chuỗi trên RAM (In-memory).
 * Tốc độ sẽ nhanh gấp nhiều lần do không phải ghi file rác ra ổ cứng.
 */
@Component
public class SequenceTrimmer {

    /**
     * 1. Thuật toán cắt theo Chất lượng (Quality Trimming)
     * Dựa theo nguyên lý của BWA (Được nhắc đến trong help file của Trim Galore):
     * "Subtract INT from all qualities; compute partial sums; cut at minimal sum index"
     */
    public void trimByQuality(Sequence seq, int qualityCutoff) {
        String qualString = seq.getQualityString();
        if (qualString == null || qualString.isEmpty()) return;

        int minSum = 0;
        int currentSum = 0;
        int bestCutPosition = qualString.length();

        // Duyệt ngược từ cuối chuỗi (3' end) lên đầu (5' end)
        for (int i = qualString.length() - 1; i >= 0; i--) {
            // Giải mã Phred33 (Ký tự ASCII -> Điểm số)
            int phredScore = qualString.charAt(i) - 33; 
            
            // Công thức BWA: Cộng dồn điểm trừ đi ngưỡng
            currentSum += (qualityCutoff - phredScore);

            // Tìm vị trí mà tổng điểm chạm đáy nhỏ nhất
            if (currentSum > minSum) {
                minSum = currentSum;
                bestCutPosition = i;
            }
        }

        // Cập nhật lại chuỗi DNA và chuỗi Quality trong RAM
        if (bestCutPosition < qualString.length()) {
            seq.setSequence(seq.getSequence().substring(0, bestCutPosition));
            seq.setQualityString(qualString.substring(0, bestCutPosition));
        }
    }

    /**
     * 2. Thuật toán cắt Adapter (Adapter Trimming)
     * Đây là phiên bản rút gọn của Semi-global Alignment. Nó tìm kiếm sự xuất hiện
     * của Adapter ở đuôi (3' end) của Sequence và cắt bỏ.
     */
    public void trimAdapter(Sequence seq, String adapterSeq, int minOverlap, double allowedErrorRate) {
        if (adapterSeq == null || adapterSeq.isEmpty()) return;

        String dna = seq.getSequence();
        int seqLen = dna.length();
        int adapterLen = adapterSeq.length();

        int bestCutIndex = -1;

        // Trượt Adapter dọc theo đuôi của sequence để tìm điểm khớp
        // Ví dụ: 
        // DNA:     ATCGATCGAGATCGG
        // Adapter:         AGATCGGAAGAGC
        for (int i = 0; i <= seqLen - minOverlap; i++) {
            int overlapLength = Math.min(seqLen - i, adapterLen);
            
            String dnaPart = dna.substring(i, i + overlapLength);
            String adapterPart = adapterSeq.substring(0, overlapLength);

            int errors = calculateMismatches(dnaPart, adapterPart);
            
            // Nếu tỉ lệ lỗi <= mức cho phép (vd: 0.1), coi như đây là adapter và cắt
            if ((double) errors / overlapLength <= allowedErrorRate) {
                bestCutIndex = i;
                break;
            }
        }

        // Thực hiện cắt
        if (bestCutIndex != -1) {
            seq.setSequence(dna.substring(0, bestCutIndex));
            seq.setQualityString(seq.getQualityString().substring(0, bestCutIndex));
        }
    }

    // Hàm phụ trợ tính số lỗi (Mismatch)
    private int calculateMismatches(String s1, String s2) {
        int mismatches = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != 'N' && s2.charAt(i) != 'N' && s1.charAt(i) != s2.charAt(i)) {
                mismatches++;
            }
        }
        return mismatches;
    }
}