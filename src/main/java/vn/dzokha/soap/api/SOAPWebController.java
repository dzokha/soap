package vn.dzokha.soap.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType; 
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import vn.dzokha.soap.dto.response.AnalysisReportDTO; 
import vn.dzokha.soap.dto.response.chart.TileDataDTO;

import vn.dzokha.soap.engine.qc.QCService;
import vn.dzokha.soap.engine.qc.modules.PerTileQualityScores;
import vn.dzokha.soap.engine.trimming.NativeTrimmingService;
import vn.dzokha.soap.engine.trimming.TrimConfig;
import vn.dzokha.soap.engine.assembly.AssemblyService; 
import vn.dzokha.soap.engine.annotation.AnnotationService; // <-- THÊM IMPORT ANNOTATION
import vn.dzokha.soap.io.parser.RawDataViewer;
import vn.dzokha.soap.io.parser.SequenceFile;
import vn.dzokha.soap.io.parser.SequenceFactory;
import vn.dzokha.soap.mapper.AnalysisReportMapper;
import vn.dzokha.soap.config.SOAPProperties; 

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/soap")
@Tag(name = "SOAP Analysis", description = "Các API liên quan đến phân tích chất lượng trình tự DNA")
public class SOAPWebController {

    private static final Logger log = LoggerFactory.getLogger(SOAPWebController.class);

    private final QCService qcService;
    private final RawDataViewer rawDataViewer;
    private final NativeTrimmingService trimmingService;
    private final SOAPProperties soapProperties; 
    private final AssemblyService assemblyService; 
    private final AnnotationService annotationService; // <-- THÊM KHAI BÁO ANNOTATION

    // CẬP NHẬT CONSTRUCTOR ĐỂ INJECT AnnotationService
    public SOAPWebController(QCService qcService, RawDataViewer rawDataViewer, 
                             NativeTrimmingService trimmingService, SOAPProperties soapProperties,
                             AssemblyService assemblyService, AnnotationService annotationService) {
        this.qcService = qcService;
        this.rawDataViewer = rawDataViewer;
        this.trimmingService = trimmingService;
        this.soapProperties = soapProperties;
        this.assemblyService = assemblyService;
        this.annotationService = annotationService;
    }

    // =========================================================================
    // API BƯỚC 1 & 3: RAW QC / CLEAN QC
    // =========================================================================
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<List<AnalysisReportDTO>>> analyze(@RequestParam("files") MultipartFile[] files) {
        log.info("Hệ thống đang xử lý {} file trình tự.", files.length);
        
        return qcService.processMultipleFiles(files).thenApply(results -> {
            List<AnalysisReportDTO> dtos = results.stream()
                    .map(AnalysisReportMapper::toDTO)
                    .collect(Collectors.toList());
            log.info("Đã phân tích xong và đóng gói kết quả cho {} file.", dtos.size());
            return ResponseEntity.ok(dtos);
        }).exceptionally(ex -> {
            log.error("Lỗi phân tích: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    // =========================================================================
    // API BƯỚC 2: TRIMMING 
    // =========================================================================
    @PostMapping(value = "/trim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> trimData(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "qualityCutoff", defaultValue = "20") int qualityCutoff,
            @RequestParam(value = "minLength", defaultValue = "35") int minLength,
            @RequestParam(value = "autoDetectAdapter", defaultValue = "true") boolean autoDetectAdapter) {

        log.info("Tiếp nhận yêu cầu Trimming {} file. Quality: {}, MinLength: {}, AutoAdapter: {}", 
                 files.length, qualityCutoff, minLength, autoDetectAdapter);

        try {
            TrimConfig config = new TrimConfig();
            config.setQualityCutoff(qualityCutoff);
            config.setMinLength(minLength);
            if (autoDetectAdapter) {
                config.setAdapterSequence("AGATCGGAAGAGC"); 
            }

            MultipartFile uploadedFile = files[0];
            File tempInputFile = File.createTempFile("upload_raw_", "_" + uploadedFile.getOriginalFilename());
            uploadedFile.transferTo(tempInputFile);

            SequenceFactory factory = new SequenceFactory(soapProperties);
            SequenceFile seqFile = factory.getSequenceFile(
                uploadedFile.getOriginalFilename(), 
                new FileInputStream(tempInputFile)
            );

            File trimmedFile = trimmingService.executeTrimming(seqFile, config);
            tempInputFile.delete();

            InputStreamResource resource = new InputStreamResource(new FileInputStream(trimmedFile));
            
            String downloadFilename = uploadedFile.getOriginalFilename()
                                        .replace(".fastq", "_trimmed.fastq")
                                        .replace(".fq", "_trimmed.fq");
            if (!downloadFilename.endsWith(".gz")) { downloadFilename += ".gz"; }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(trimmedFile.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Lỗi trong quá trình Trimming: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // API BƯỚC 4: ASSEMBLY (LẮP RÁP BỘ GENE)
    // =========================================================================
    @PostMapping(value = "/assemble", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> assembleData(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "assembler", defaultValue = "Unicycler") String assembler) {

        log.info("Tiếp nhận yêu cầu Assembly {} file bằng thuật toán {}", files.length, assembler);

        try {
            File assembledFastaFile = assemblyService.executeAssembly(files, assembler);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(assembledFastaFile));
            
            String baseName = files[0].getOriginalFilename() != null ? files[0].getOriginalFilename().split("\\.")[0] : "sample";
            String downloadFilename = baseName + "_" + assembler.toLowerCase() + "_assembled.fasta";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(assembledFastaFile.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Lỗi trong quá trình Assembly: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // API BƯỚC 5: ANNOTATION (CHÚ GIẢI BỘ GENE)
    // =========================================================================
    @PostMapping(value = "/annotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> annotateData(@RequestParam("files") MultipartFile[] files) {

        log.info("Tiếp nhận yêu cầu Annotation {} file Fasta.", files.length);

        try {
            // 1. Gọi AnnotationService xử lý dữ liệu
            File annotatedGbkFile = annotationService.executeAnnotation(files);

            // 2. Chuyển file thành stream để đẩy về Trình duyệt
            InputStreamResource resource = new InputStreamResource(new FileInputStream(annotatedGbkFile));
            
            // 3. Đặt tên file xuất ra (thường là .gbk)
            String baseName = files[0].getOriginalFilename() != null ? files[0].getOriginalFilename().split("\\.")[0] : "sample";
            String downloadFilename = baseName + "_annotated.gbk";

            // 4. Trả về phản hồi tải file (Attachment)
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(annotatedGbkFile.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Lỗi trong quá trình Annotation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // API BƯỚC 6: VISUALIZATION (SNAPGENE ALTERNATIVE TẠI WEB)
    // =========================================================================
    @PostMapping(value = "/visualize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> visualizeGenome(@RequestParam("file") MultipartFile file) {
        log.info("Tiếp nhận yêu cầu Trực quan hóa bản đồ gene (SnapGene Web Alternative) cho file: {}", file.getOriginalFilename());

        try {
            // Đọc toàn bộ nội dung file .gbk (GenBank) hoặc .fasta thành chuỗi văn bản (Plain Text)
            // Giao diện Web (JavaScript / React / SeqViz) sẽ lấy chuỗi này để tự động vẽ bản đồ
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            return ResponseEntity.ok(content);

        } catch (Exception e) {
            log.error("Lỗi khi đọc file bộ gene: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống khi đọc file GenBank/Fasta: " + e.getMessage());
        }
    }

    // =========================================================================
    // CÁC TIỆN ÍCH KHÁC
    // =========================================================================
    @GetMapping("/per-tile-quality")
    public ResponseEntity<TileDataDTO> getPerTileQuality() {
        try {
            PerTileQualityScores module = qcService.getModule(PerTileQualityScores.class);
            if (module == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(module.getChartData());
        } catch (Exception e) {
            log.error("Không thể lấy dữ liệu Tile Quality: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("SOAP Analysis Service is active (Java 21/25).");
    }

    @PostMapping(value = "/view-raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> viewRawData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        
        log.info("Yêu cầu xem dữ liệu thô cho file: {}, giới hạn: {} đoạn", file.getOriginalFilename(), limit);

        try {
            List<String> rawLines = rawDataViewer.getRawFastqFromStream(file.getInputStream(), limit);
            
            String result = String.join("\n", rawLines);
            if (rawLines.isEmpty()) {
                return ResponseEntity.ok("File không có dữ liệu hoặc định dạng không đúng.");
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Lỗi khi đọc dữ liệu thô: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể đọc file: " + e.getMessage());
        }
    }
}