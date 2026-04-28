package vn.dzokha.soap.service;

// 2 dòng import cực kỳ quan trọng để sửa lỗi của bạn:
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

// Import các Service "làm thuê" từ các thư mục khác
import vn.dzokha.soap.engine.qc.QCService; // Giả sử bạn đã đổi tên AnalysisService thành QCService
import vn.dzokha.soap.engine.trimming.NativeTrimmingService;
import vn.dzokha.soap.engine.trimming.TrimConfig;
import vn.dzokha.soap.io.parser.SequenceFile; // Import SequenceFile theo cấu trúc mới

@Service
public class PipelineOrchestrator {
    
    @Autowired 
    private QCService qcService;
    
    @Autowired 
    private NativeTrimmingService trimmingService;
    
    /**
     * Hàm điều phối toàn bộ quy trình
     */
    public void runFullAnalysis(SequenceFile file) {
        
        // 1. Chạy QC lần đầu
        // qcService.processUploadedFile(file); // (Tên hàm tùy thuộc vào code của bạn)
        
        // 2. Chạy Cắt Trimming
        TrimConfig config = new TrimConfig();
        // trimmingService.executeTrimming(file, config);
        
        // 3. Chạy QC lại lần nữa (Clean QC)
        // qcService.processUploadedFile(file);
        
    }
}