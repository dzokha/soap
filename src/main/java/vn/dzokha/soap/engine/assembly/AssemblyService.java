package vn.dzokha.soap.engine.assembly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Service quản lý tiến trình Lắp ráp hệ gene (Genome Assembly).
 * Tương lai sẽ dùng ProcessBuilder để gọi trực tiếp các lệnh Linux như `spades.py` hoặc `unicycler`.
 */
@Service
public class AssemblyService {

    private static final Logger log = LoggerFactory.getLogger(AssemblyService.class);

    public File executeAssembly(MultipartFile[] files, String assemblerType) throws Exception {
        log.info("Bắt đầu quá trình Lắp ráp (Assembly) bằng thuật toán: {}", assemblerType);
        log.info("Số lượng file đầu vào: {}", files.length);

        /* ====================================================================
         * TODO TƯƠNG LAI: TÍCH HỢP CÔNG CỤ THẬT
         * 1. Lưu files[] ra thư mục tạm (temp/assembly_input)
         * 2. Dùng ProcessBuilder gọi lệnh hệ thống:
         * - SPAdes: "spades.py -1 read1.fastq.gz -2 read2.fastq.gz -o output_dir"
         * - Unicycler: "unicycler -1 read1.fastq.gz -2 read2.fastq.gz -o output_dir"
         * 3. Chờ tiến trình chạy xong, tìm file "scaffolds.fasta" hoặc "assembly.fasta"
         * 4. Trả file đó về.
         * ==================================================================== */

        // GIẢ LẬP (MOCK) ĐỂ TEST LUỒNG UI: Dừng 3 giây tạo cảm giác đang chạy thuật toán
        Thread.sleep(3000);

        // Tạo một file FASTA mẫu (Định dạng chuẩn của bộ gene đã lắp ráp)
        File outputFile = File.createTempFile("assembly_mock_" + assemblerType.toLowerCase() + "_", ".fasta");
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Định dạng FASTA luôn bắt đầu bằng dấu > kèm tên Node/Contig
            writer.write(">NODE_1_length_54321_cov_14.2_circular=true\n");
            writer.write("ATGCGTACGTAGCTAGCTAGCATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATC\n");
            writer.write("CGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGATCGAT\n");
            writer.write("GGCCTTAAGGCCttaaggccttaaggccttaaggccttaaggccttaaggccttaaggccttaa\n");
            writer.write(">NODE_2_length_1250_cov_5.1\n");
            writer.write("TTGACCGATGACCGTACGATCGTACGATCGTACGATCGTACGATCGTACGATCGTACGATCGTA\n");
        }

        log.info("Hoàn tất lắp ráp bằng {}. File kết quả: {}", assemblerType, outputFile.getAbsolutePath());
        
        // Trả file FASTA này về cho Controller đẩy xuống Trình duyệt
        return outputFile;
    }
}