package vn.dzokha.soap.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;


// Import thiếu khiến Maven báo lỗi
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import vn.dzokha.soap.engine.qc.core.ModuleFactory;
import vn.dzokha.soap.engine.qc.core.QCModule;
import vn.dzokha.soap.io.parser.SequenceFactory;
import vn.dzokha.soap.io.parser.SequenceFile;
import vn.dzokha.soap.domain.job.AnalysisResult;
import vn.dzokha.soap.dto.request.AnalysisRequestDTO;
import vn.dzokha.soap.config.SOAPProperties; 


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final Executor analysisTaskExecutor;
    private final SOAPProperties properties;
    private final ModuleFactory moduleFactory;
    private final SequenceFactory sequenceFactory;
    private final AnalysisRunner analysisRunner;
    private final ApplicationContext applicationContext; // Thêm biến này


    @Autowired    
    public AnalysisService(SOAPProperties properties, 
                           @Qualifier("analysisTaskExecutor") Executor executor,
                           ModuleFactory moduleFactory,
                           SequenceFactory sequenceFactory,
                           AnalysisRunner analysisRunner,
                           ApplicationContext applicationContext) {
        this.properties = properties;
        this.analysisTaskExecutor = executor;
        this.moduleFactory = moduleFactory;
        this.sequenceFactory = sequenceFactory;
        this.analysisRunner = analysisRunner;
        this.applicationContext = applicationContext; // Gán giá trị
        System.out.println("Upload directory: " + properties.getUploadDir());
    }

    /**
     * Hàm in ra log trạng thái phần cứng và mức độ "ăn" tài nguyên của hệ thống
     */
    private void logSystemResources(String giaiDoan) {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        Runtime runtime = Runtime.getRuntime();

        // 1. Lấy thông tin CPU
        int cores = runtime.availableProcessors();
        double cpuLoadToanMay = osBean.getCpuLoad() * 100; // % CPU của cả máy tính
        double cpuLoadUngDung = osBean.getProcessCpuLoad() * 100; // % CPU mà riêng phần mềm SOAP đang chiếm

        // 2. Lấy thông tin RAM (chuyển từ Byte sang Megabyte)
        long maxRam = runtime.maxMemory() / (1024 * 1024);     // Giới hạn RAM tối đa JVM được phép xài
        long totalRam = runtime.totalMemory() / (1024 * 1024);   // RAM hệ thống đã cấp cho ứng dụng
        long freeRam = runtime.freeMemory() / (1024 * 1024);     // RAM ứng dụng chưa xài tới
        long usedRam = totalRam - freeRam;                       // RAM đang thực sự chứa dữ liệu

        log.info("[BÁO CÁO TÀI NGUYÊN] - Thời điểm: {}", giaiDoan);
        log.info("   CPU Cores (Số nhân): {}", cores);
        log.info("   CPU Load (Toàn hệ thống): {} %", String.format("%.2f", cpuLoadToanMay));
        log.info("   CPU Load (Riêng app SOAP): {} %", String.format("%.2f", cpuLoadUngDung));
        log.info("   RAM JVM Đang dùng: {} MB / Đã cấp {} MB (Giới hạn đỉnh: {} MB)", usedRam, totalRam, maxRam);
        log.info("--------------------------------------------------");
    }


    /**
     * Xử lý phân tích nhiều file cùng lúc
     */
    public CompletableFuture<List<AnalysisResult>> processMultipleFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        // ĐO LẦN 1: Tình trạng lúc máy đang rảnh (Trước khi chạy)
        logSystemResources("TRƯỚC KHI BẮT ĐẦU PHÂN TÍCH " + files.length + " FILE");
        List<CompletableFuture<AnalysisResult>> futures = new ArrayList<>();
        AnalysisService selfProxy = applicationContext.getBean(AnalysisService.class);
        for (MultipartFile file : files) {
            futures.add(selfProxy.processUploadedFile(file));
            // futures.add(this.processUploadedFile(file));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // ĐO LẦN 2: Ngay khoảnh khắc các luồng vừa chạy xong (RAM đang căng nhất)
                    logSystemResources("NGAY SAU KHI PHÂN TÍCH XONG - ĐANG ĐÓNG GÓI JSON");

                    List<AnalysisResult> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                    
                    // Gọi Garbage Collector dọn dẹp (Chỉ dùng để test, thực tế không nên gọi tay)
                    System.gc(); 
                    
                    // ĐO LẦN 3: Sau khi dọn rác
                    logSystemResources("SAU KHI DỌN DẸP RÁC BỘ NHỚ (GC)");
                    
                    return results;
                });
    }


    @Async("analysisTaskExecutor") 
    public CompletableFuture<AnalysisResult> processUploadedFile(MultipartFile uploadFile) {
        String fileName = uploadFile.getOriginalFilename();
        log.info("SOAP đang bắt đầu phân tích file: {}", fileName);
        
        try (SequenceFile sequenceFile = sequenceFactory.getSequenceFile(fileName, uploadFile.getInputStream())) {
            
            // Lấy danh sách các module phân tích (Basic Stats, Per Base Quality, v.v.)
            QCModule[] modules = moduleFactory.getStandardModuleList();
            
            // Chạy phân tích đồng bộ trong luồng Async này
            analysisRunner.runAnalysisSync(sequenceFile, modules);

            log.info("Hoàn thành phân tích cho: {}", fileName);
            return CompletableFuture.completedFuture(
                new AnalysisResult(fileName, modules, "SUCCESS")
            );

        } catch (Exception e) {
            log.error("Lỗi khi xử lý file {}: {}", fileName, e.getMessage());
            return CompletableFuture.completedFuture(
                new AnalysisResult(fileName, null, "FAILED: " + e.getMessage())
            );
        }
    }

    // public void startProcess(AnalysisRequestDTO request) {
    //     // LOG: Giúp theo dõi request trong log file
    //     System.out.println("Tiếp nhận yêu cầu phân tích: " + request.toString());

    //     // Hợp nhất tham số (Logic: Ưu tiên DTO từ Frontend > YML hệ thống)
    //     // Việc kiểm tra null giúp hệ thống linh hoạt: User không truyền thì dùng mặc định
    //     int finalKmer = (request.getKmerSize() != null) 
    //                     ? request.getKmerSize() 
    //                     : properties.getAnalysis().getKmerSize();
        
    //     boolean finalCasava = (request.getCasava() != null) 
    //                           ? request.getCasava() 
    //                           : properties.getAnalysis().isCasava();
        
    //     int finalMinLength = (request.getMinLength() != null)
    //                          ? request.getMinLength()
    //                          : properties.getAnalysis().getMinLength();

    //     // Chạy đa luồng qua TaskExecutor để không làm treo luồng HTTP chính
    //     analysisTaskExecutor.execute(() -> {
    //         try {
    //             // Giả lập logic xử lý SOAP thực tế (ví dụ: thực thi lệnh shell hoặc thư viện bio-java)
    //             System.out.println(">>> ĐANG PHÂN TÍCH: " + request.getFilename());
    //             System.out.println(">>> THAM SỐ: Kmer=" + finalKmer + ", Casava=" + finalCasava);
                
    //             // [Nơi gọi các hàm xử lý DNA của bạn]
                
    //             System.out.println(">>> HOÀN THÀNH: " + request.getFilename());
    //         } catch (Exception e) {
    //             System.err.println("Lỗi trong quá trình phân tích file " + request.getFilename() + ": " + e.getMessage());
    //         }
    //     });
    // }

    // Thêm vào trong class AnalysisService
    public <T> T getModule(Class<T> moduleClass) {
        // Giả sử bạn lưu trữ các module trong một danh sách hoặc map sau khi phân tích
        // Ở đây ta tìm module có kiểu tương ứng trong danh sách modules của kết quả gần nhất
        // Hoặc lấy trực tiếp từ Bean Context nếu bạn quản lý module như Spring Beans
        return applicationContext.getBean(moduleClass); 
    }
}