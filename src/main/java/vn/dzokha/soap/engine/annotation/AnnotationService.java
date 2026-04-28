package vn.dzokha.soap.engine.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Service quản lý tiến trình Chú giải hệ gene (Genome Annotation).
 * Tương lai sẽ dùng ProcessBuilder để gọi trực tiếp lệnh Linux `pharokka`.
 */
@Service
public class AnnotationService {

    private static final Logger log = LoggerFactory.getLogger(AnnotationService.class);

    public File executeAnnotation(MultipartFile[] files) throws Exception {
        log.info("Bắt đầu quá trình Chú giải (Annotation) với Pharokka");
        log.info("Số lượng file đầu vào: {}", files.length);

        /* ====================================================================
         * TODO TƯƠNG LAI: TÍCH HỢP PHAROKKA THẬT
         * 1. Lưu file Fasta ra thư mục tạm
         * 2. Dùng ProcessBuilder gọi lệnh: "pharokka.py -i input.fasta -o output_dir"
         * 3. Chờ tiến trình chạy xong, tìm file "pharokka.gbk"
         * 4. Trả file đó về.
         * ==================================================================== */

        // GIẢ LẬP (MOCK): Dừng 3 giây tạo cảm giác đang chạy thuật toán
        Thread.sleep(3000);

        // Tạo một file GenBank (.gbk) mẫu
        File outputFile = File.createTempFile("annotation_mock_", ".gbk");
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("LOCUS       NODE_1_mock_phage              1050 bp    DNA     linear   PHG 29-APR-2026\n");
            writer.write("DEFINITION  Mock phage genome assembly, complete sequence.\n");
            writer.write("ACCESSION   NODE_1\n");
            writer.write("VERSION     NODE_1\n");
            writer.write("KEYWORDS    .\n");
            writer.write("SOURCE      Bacteriophage\n");
            writer.write("  ORGANISM  Bacteriophage\n");
            writer.write("            Viruses; Duplodnaviria; Heunggaviria; Uroviricota.\n");
            writer.write("FEATURES             Location/Qualifiers\n");
            writer.write("     source          1..1050\n");
            writer.write("                     /organism=\"Bacteriophage\"\n");
            writer.write("                     /mol_type=\"genomic DNA\"\n");
            writer.write("     CDS             150..850\n");
            writer.write("                     /product=\"major capsid protein\"\n");
            writer.write("                     /translation=\"MSEQIRT...\"\n");
            writer.write("ORIGIN      \n");
            writer.write("        1 atgcgtacgt agctagctag catcgatcga tcgatcgatc gatcgatcga tcgatcgatc\n");
        }

        log.info("Hoàn tất Annotation. File kết quả: {}", outputFile.getAbsolutePath());
        
        // Trả file GBK này về cho Controller đẩy xuống Trình duyệt
        return outputFile;
    }
}