vn.dzokha.soap/
│
├── api/                         (Đổi từ controller) - Chứa SOAPWebController.java
├── config/                      (Cấu hình Spring, Security, CORS)
├── exception/                   (Xử lý lỗi Global)
│
├── core/                        
│   └── utils/                  
│
├── dto/                         ⭐ Nơi ở của DTO
│   ├── request/                 (Ví dụ: AnalysisRequestDTO.java)
│   └── response/                (Ví dụ: AnalysisReportDTO.java, TileDataDTO.java)
│
├── mapper/                      ⭐ Nơi ở của Mapper
│   └── AnalysisReportMapper.java (Chuyển từ AnalysisResult sang DTO)
│
├── service/                     ⭐ Nơi ở của Service (Tầng Điều phối/Orchestration)
│   ├── AnalysisService.java     (Nhận lệnh từ Controller, gọi Runner, quản lý luồng)
│   └── AnalysisRunner.java      (Vòng lặp đọc file và đẩy vào các Module QC)
│
├── domain/                      ⭐ Nơi ở của Dữ liệu Lõi (Mới tách ra)
│   ├── sequence/                (Sequence.java, PhredEncoding.java...)
│   └── job/                     (Sau này thêm Job.java, AnalysisResult.java)
│
├── engine/                      ⭐ Nơi ở của Xưởng Thuật toán (Mới tách ra)
│ 	├── qc/                      
│   │   ├── core/                (QCModule.java, ModuleConfig.java...)
│   │   ├── modules/             (BasicStats.java, PerBaseQualityScores.java...)
│   │   └── util/                (BaseGroup.java, ContaminantFinder.java...)
│   │
│   ├── trimming/                # ⚠️ Trimming Service (Interface) + NativeTrimmer (nếu bạn muốn tự viết)
│   │
│   ├── assembly/                # ⚠️ Assembly Service (Interface) + GraphOptimizer (Java tự viết)
│   │
│   └── annotation/              # ⚠️ Annotation Service (Interface) + CustomHMMFinder (Java tự viết)
│
├── integration/                 # TẦNG KẾT NỐI BÊN NGOÀI (Adapters / Wrappers)
│   │                            # Quy tắc: Code gọi tool ngoài bị nhốt ở đây, không được rò rỉ ra ngoài
│   │
│   ├── trimgalore/              # Wrapper gọi tiến trình TrimGalore/Cutadapt
│   ├── spades/                  # Wrapper gọi tiến trình SPAdes / Unicycler
│   └── pharokka/                # Wrapper gọi tiến trình Pharokka
│
└── io/                          ⭐ Nơi ở của I/O (Mới tách ra)
    ├── parser/                  (FastQFile.java, SequenceFile.java...)
    └── filter/                  (FileFilters...)