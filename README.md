# 🧬 SOAP: Sequence Optimization and Analysis Pipeline

SOAP là một nền tảng phần mềm mã nguồn mở được thiết kế để phân tích, đánh giá chất lượng (QC) và tối ưu hóa dữ liệu Giải trình tự gen thế hệ mới (NGS - High-Throughput Sequencing).

Với khả năng xử lý mạnh mẽ các định dạng dữ liệu thô như **FASTQ** và **BAM**, SOAP cung cấp cái nhìn chi tiết và toàn diện về chất lượng thư viện gen, giúp các nhà nghiên cứu phát hiện sớm sai sót kỹ thuật trước khi tiến hành các bước lắp ráp và phân tích hạ nguồn phức tạp.

---

## 💡 Tại sao lại chọn SOAP?

Trong kỷ nguyên Genomics, dữ liệu thô thường chứa đựng các định kiến kỹ thuật (biases), tạp nhiễm adapter hoặc suy giảm chất lượng hóa chất giải trình tự.

SOAP đóng vai trò như một **"màng lọc thông minh"**, thực hiện một chuỗi các thử nghiệm thống kê nghiêm ngặt để đảm bảo tính toàn vẹn của dữ liệu.

Mỗi module phân tích trong SOAP được đánh giá trực quan qua hệ thống chỉ báo trạng thái:

* ✅ **Pass (Đạt):** Dữ liệu hoàn toàn bình thường, nằm trong ngưỡng phân phối chuẩn.
* ⚠️ **Warning (Cảnh báo):** Có dấu hiệu bất thường (ví dụ: định kiến mồi ngẫu nhiên, nhiễm adapter nhẹ), cần lưu ý ở các bước sau.
* ❌ **Fail (Thất bại):** Sai lệch nghiêm trọng, thư viện bị lỗi nặng và có thể cần giải trình tự lại.

---

## 🚀 Các tính năng cốt lõi

### 1. Phân tích chất lượng theo thời gian thực (Per Base Quality)

Đo lường điểm chất lượng **Phred** tại mỗi chu kỳ giải trình tự (sequencing cycle).
Hệ thống giúp xác định chính xác thời điểm tín hiệu huỳnh quang bắt đầu suy giảm, từ đó đưa ra quyết định cắt tỉa (trimming) hợp lý.

---

### 2. Nhận diện tạp nhiễm (Contamination Detection)

Tự động đối chiếu các trình tự xuất hiện quá mức (**Overrepresented sequences**) với cơ sở dữ liệu tạp nhiễm phổ biến:

* Adapter dimers (Illumina/Nanopore)
* Primer sequences
* rRNA sequences

---

### 3. Đánh giá phân phối GC Content

SOAP phân tích tỷ lệ:

(G + C) / (A + T + G + C)

và so sánh với mô hình **phân phối chuẩn (Normal Distribution)** của sinh vật mục tiêu.

👉 Tính năng này rất nhạy trong việc phát hiện DNA ngoại lai.

---

### 4. Phân tích K-mer và định kiến vị trí

Sử dụng **Binomial test** để phát hiện sự tích tụ bất thường của các đoạn trình tự ngắn (K-mer).

Giúp phát hiện:

* Lỗi PCR
* Primer bias
* Adapter chưa cắt

---

## 🛠️ Ngăn xếp công nghệ (Tech Stack)

### Backend (Lõi phân tích)

* **Java 21**
* **Spring Boot 3.x**
* Multi-threading (tối ưu I/O)

### Frontend (Giao diện)

* **ReactJS + Vite + TypeScript**
* **D3.js** (trực quan hóa sinh tin học)
* **Tailwind CSS**

### Hạ tầng (Infrastructure)

* Cloudflare DNS
* GitHub Actions (CI/CD)

---

## ⚙️ Hướng dẫn cài đặt

### Yêu cầu
* git: https://git-scm.com/
* Java 21: https://www.oracle.com/java/technologies/downloads/
* Maven
* Node.js >= 18

---

### 1. Clone dự án

```bash
git clone https://github.com/dzokha/soap.git
cd soap
```

---

### 2. Khởi chạy Backend (Spring Boot)

```bash
# Trên Windows (Dùng Command Prompt / PowerShell)
mvnw.cmd spring-boot:run

# Trên Linux/Mac (Terminal)
chmod +x mvnw
./mvnw spring-boot:run

```

👉 Backend chạy tại:
`http://localhost:2026`

---

### 3. Khởi chạy Frontend (nếu có)

```bash
cd frontend
npm install
npm run dev
```

---

## 🤝 Đóng góp (Contributing)

Chúng tôi luôn chào đón sự đóng góp từ cộng đồng Bioinformatics và Software Engineering.

Bạn có thể:

* Mở **Issue** để báo lỗi hoặc đề xuất
* Fork repo và gửi **Pull Request**

---

## 📄 Giấy phép (License)

SOAP là phần mềm mã nguồn mở theo giấy phép:

**GNU General Public License v3.0 (GPL-3.0)**

Bạn có thể:

* Sử dụng
* Nghiên cứu
* Sửa đổi
* Phân phối lại

👉 Với điều kiện giữ nguyên giấy phép GPL.

Xem chi tiết tại file `LICENSE`.

---

## 👨‍💻 Tác giả

**Nguyễn Văn Kha**

Chuyên viên – Phòng Sở hữu trí tuệ

Sở Khoa học và Công nghệ TP. Cần Thơ, Việt Nam

📫 Email: [dzokha1010@gmail.com](mailto:dzokha1010@gmail.com)
