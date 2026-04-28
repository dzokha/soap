package vn.dzokha.soap.engine.trimming;

import org.springframework.stereotype.Service;
import vn.dzokha.soap.domain.sequence.Sequence;
import vn.dzokha.soap.io.parser.SequenceFile;
import vn.dzokha.soap.io.parser.SequenceFormatException; 
import vn.dzokha.soap.io.writer.FastQWriter; // IMPORT CLASS GHI FILE

import java.io.File; // IMPORT XỬ LÝ FILE
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service này là "Nhạc trưởng" của quá trình Trimming.
 * Nó sẽ MƯỢN SequenceFile từ thư mục IO để đọc, giao cho SequenceTrimmer cắt,
 * và gọi IO Writer để lưu kết quả ra file.
 */
@Service
public class NativeTrimmingService {

    private static final Logger log = LoggerFactory.getLogger(NativeTrimmingService.class);
    private final SequenceTrimmer trimmerAlgo;

    public NativeTrimmingService(SequenceTrimmer trimmerAlgo) {
        this.trimmerAlgo = trimmerAlgo;
    }

    /**
     * Hàm thực thi cắt dữ liệu 100% Native Java.
     * TRẢ VỀ: File (.fastq.gz) đã được làm sạch và nén lại.
     */
    public File executeTrimming(SequenceFile inFile, TrimConfig config) throws Exception {
        int totalReads = 0;
        int droppedReads = 0;
        
        log.info("Bắt đầu Native Trimming (In-memory)... Adapter: {}", config.getAdapterSequence());

        // 1. Tạo file tạm thời trên RAM/Đĩa để chứa dữ liệu xuất ra
        File outputFile = File.createTempFile("trimmed_output_", ".fastq.gz");

        // 2. Sử dụng try-with-resources để mở FastQWriter và tự động đóng/giải phóng bộ nhớ khi xong
        try (FastQWriter writer = new FastQWriter(outputFile, true)) {
            while (inFile.hasNext()) {
                Sequence seq = inFile.next();
                totalReads++;

                // Cắt theo chất lượng (Quality Trimming)
                if (config.getQualityCutoff() > 0) {
                    trimmerAlgo.trimByQuality(seq, config.getQualityCutoff());
                }

                // Cắt Adapter
                if (config.getAdapterSequence() != null) {
                    trimmerAlgo.trimAdapter(seq, config.getAdapterSequence(), config.getMinOverlap(), config.getErrorRate());
                }

                // Lọc theo độ dài tối thiểu
                if (seq.getSequence().length() < config.getMinLength()) {
                    droppedReads++;
                    continue; // Bỏ qua đoạn DNA này, không lưu
                }

                // Giao cho Tầng Writer để ghi xuống file
                writer.write(seq);
            }
        } catch (SequenceFormatException e) {
            log.error("Lỗi định dạng chuỗi DNA khi đọc file: {}", e.getMessage());
        }

        log.info("Hoàn tất Trimming! Tổng số reads: {}. Bị loại do quá ngắn: {}", totalReads, droppedReads);
        
        // 3. Trả file đã ghi xong về cho Controller
        return outputFile;
    }
}