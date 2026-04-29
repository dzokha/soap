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
import vn.dzokha.soap.engine.annotation.AnnotationService; 
import vn.dzokha.soap.io.parser.RawDataViewer;
import vn.dzokha.soap.io.parser.SequenceFile;
import vn.dzokha.soap.io.parser.SequenceFactory;
import vn.dzokha.soap.mapper.AnalysisReportMapper;
import vn.dzokha.soap.config.SOAPProperties; 
import vn.dzokha.soap.config.QCConfigManager; // <-- THÊM IMPORT

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
    private final AnnotationService annotationService; 
    private final QCConfigManager qcConfigManager; // <-- KHAI BÁO BIẾN

    // CẬP NHẬT CONSTRUCTOR ĐỂ INJECT QCConfigManager
    public SOAPWebController(QCService qcService, RawDataViewer rawDataViewer, 
                             NativeTrimmingService trimmingService, SOAPProperties soapProperties,
                             AssemblyService assemblyService, AnnotationService annotationService,
                             QCConfigManager qcConfigManager) {
        this.qcService = qcService;
        this.rawDataViewer = rawDataViewer;
        this.trimmingService = trimmingService;
        this.soapProperties = soapProperties;
        this.assemblyService = assemblyService;
        this.annotationService = annotationService;
        this.qcConfigManager = qcConfigManager;
    }

    // =========================================================================
    // API BƯỚC 1 & 3: RAW QC / CLEAN QC (BẮT THAM SỐ TÙY CHỈNH LIMITS)
    // =========================================================================
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<List<AnalysisReportDTO>>> analyze(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "limits_adapterWarn", required = false) Double adapterWarn,
            @RequestParam(value = "limits_adapterError", required = false) Double adapterError,
            @RequestParam(value = "limits_nWarn", required = false) Double nWarn,
            @RequestParam(value = "limits_nError", required = false) Double nError,
            @RequestParam(value = "limits_dupWarn", required = false) Double dupWarn,
            @RequestParam(value = "limits_dupError", required = false) Double dupError) {

        log.info("Hệ thống đang xử lý {} file trình tự.", files.length);

        // NẾU NGƯỜI DÙNG CÓ GỬI LIMITS TỪ MODAL LÊN -> CẬP NHẬT VÀO HỆ THỐNG
        if (qcConfigManager != null) {
            if (adapterWarn != null) qcConfigManager.updateLimit("adapter", "warn", adapterWarn);
            if (adapterError != null) qcConfigManager.updateLimit("adapter", "error", adapterError);
            if (nWarn != null) qcConfigManager.updateLimit("n_content", "warn", nWarn);
            if (nError != null) qcConfigManager.updateLimit("n_content", "error", nError);
            if (dupWarn != null) qcConfigManager.updateLimit("duplication", "warn", dupWarn);
            if (dupError != null) qcConfigManager.updateLimit("duplication", "error", dupError);
        }
        
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
    // CÁC HÀM CÒN LẠI (TRIMMING, ASSEMBLY, ANNOTATION, VISUALIZE) GIỮ NGUYÊN
    // =========================================================================
    @PostMapping(value = "/trim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> trimData(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "qualityCutoff", defaultValue = "20") int qualityCutoff,
            @RequestParam(value = "minLength", defaultValue = "35") int minLength,
            @RequestParam(value = "autoDetectAdapter", defaultValue = "true") boolean autoDetectAdapter) {
        log.info("Tiếp nhận yêu cầu Trimming {} file.", files.length);
        try {
            TrimConfig config = new TrimConfig();
            config.setQualityCutoff(qualityCutoff);
            config.setMinLength(minLength);
            if (autoDetectAdapter) config.setAdapterSequence("AGATCGGAAGAGC"); 

            MultipartFile uploadedFile = files[0];
            File tempInputFile = File.createTempFile("upload_raw_", "_" + uploadedFile.getOriginalFilename());
            uploadedFile.transferTo(tempInputFile);

            SequenceFactory factory = new SequenceFactory(soapProperties);
            SequenceFile seqFile = factory.getSequenceFile(uploadedFile.getOriginalFilename(), new FileInputStream(tempInputFile));

            File trimmedFile = trimmingService.executeTrimming(seqFile, config, autoDetectAdapter);
            tempInputFile.delete();

            InputStreamResource resource = new InputStreamResource(new FileInputStream(trimmedFile));
            String downloadFilename = uploadedFile.getOriginalFilename().replace(".fastq", "_trimmed.fastq").replace(".fq", "_trimmed.fq");
            if (!downloadFilename.endsWith(".gz")) downloadFilename += ".gz"; 

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(trimmedFile.length())
                    .body(resource);
        } catch (Exception e) {
            log.error("Lỗi Trimming: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/assemble", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> assembleData(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "assembler", defaultValue = "Unicycler") String assembler) {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/annotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> annotateData(@RequestParam("files") MultipartFile[] files) {
        try {
            File annotatedGbkFile = annotationService.executeAnnotation(files);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(annotatedGbkFile));
            String baseName = files[0].getOriginalFilename() != null ? files[0].getOriginalFilename().split("\\.")[0] : "sample";
            String downloadFilename = baseName + "_annotated.gbk";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(annotatedGbkFile.length())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/visualize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> visualizeGenome(@RequestParam("file") MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/per-tile-quality")
    public ResponseEntity<TileDataDTO> getPerTileQuality() {
        try {
            PerTileQualityScores module = qcService.getModule(PerTileQualityScores.class);
            if (module == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(module.getChartData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("SOAP Analysis Service is active.");
    }

    @PostMapping(value = "/view-raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> viewRawData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        try {
            List<String> rawLines = rawDataViewer.getRawFastqFromStream(file.getInputStream(), limit);
            String result = String.join("\n", rawLines);
            return rawLines.isEmpty() ? ResponseEntity.ok("File không hợp lệ.") : ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi đọc file: " + e.getMessage());
        }
    }
}