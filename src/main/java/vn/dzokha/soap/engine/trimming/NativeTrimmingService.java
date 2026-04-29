package vn.dzokha.soap.engine.trimming;

import org.springframework.stereotype.Service;
import vn.dzokha.soap.domain.sequence.Sequence;
import vn.dzokha.soap.io.parser.SequenceFile;
import vn.dzokha.soap.io.parser.SequenceFormatException; 
import vn.dzokha.soap.io.writer.FastQWriter; 
import vn.dzokha.soap.config.QCConfigManager; // IMPORT QUẢN LÝ CẤU HÌNH

import java.io.File;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NativeTrimmingService {

    private static final Logger log = LoggerFactory.getLogger(NativeTrimmingService.class);
    private final SequenceTrimmer trimmerAlgo;
    private final QCConfigManager qcConfigManager; // THÊM BIẾN NÀY

    public NativeTrimmingService(SequenceTrimmer trimmerAlgo, QCConfigManager qcConfigManager) {
        this.trimmerAlgo = trimmerAlgo;
        this.qcConfigManager = qcConfigManager;
    }

    public File executeTrimming(SequenceFile inFile, TrimConfig config, boolean autoDetectAdapter) throws Exception {
        int totalReads = 0;
        int droppedReads = 0;
        int adapterFoundCount = 0;
        
        log.info("Bắt đầu Native Trimming...");

        File outputFile = File.createTempFile("trimmed_output_", ".fastq.gz");

        try (FastQWriter writer = new FastQWriter(outputFile, true)) {
            while (inFile.hasNext()) {
                Sequence seq = inFile.next();
                totalReads++;

                // 1. Cắt theo chất lượng
                if (config.getQualityCutoff() > 0) {
                    trimmerAlgo.trimByQuality(seq, config.getQualityCutoff());
                }

                // 2. Tự động rà quét và cắt Adapter từ file adapters.txt
                if (autoDetectAdapter) {
                    // Quét qua toàn bộ danh sách Adapter đã load từ file
                    for (Map.Entry<String, String> entry : qcConfigManager.getAllAdapters().entrySet()) {
                        String adapterSeq = entry.getValue();
                        // Trimmer sẽ trả về boolean hoặc bạn có thể kiểm tra xem seq có bị ngắn đi không
                        int oldLength = seq.getSequence().length();
                        trimmerAlgo.trimAdapter(seq, adapterSeq, config.getMinOverlap(), config.getErrorRate());
                        
                        if (seq.getSequence().length() < oldLength) {
                            adapterFoundCount++;
                            break; // Nếu tìm thấy và cắt 1 adapter rồi thì dừng vòng lặp cho đoạn gen này để tối ưu tốc độ
                        }
                    }
                }

                // 3. Lọc theo độ dài
                if (seq.getSequence().length() < config.getMinLength()) {
                    droppedReads++;
                    continue; 
                }

                writer.write(seq);
            }
        } catch (SequenceFormatException e) {
            log.error("Lỗi định dạng chuỗi DNA: {}", e.getMessage());
        }

        log.info("Hoàn tất Trimming! Tổng số reads: {}. Bị loại: {}. Số lần phát hiện Adapter: {}", 
                 totalReads, droppedReads, adapterFoundCount);
        
        return outputFile;
    }
}