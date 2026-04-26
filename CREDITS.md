## 🙏 Credits and Acknowledgments

Dự án **SOAP (Sequence Optimization and Analysis Pipeline)** được xây dựng dựa trên triết lý mã nguồn mở. Chúng tôi không thể hoàn thành dự án này nếu không có những đóng góp quan trọng từ cộng đồng phát triển phần mềm và Tin sinh học quốc tế.

Dưới đây là danh sách các công cụ, thư viện và dự án mã nguồn mở đã được sử dụng hoặc tích hợp trong SOAP:

---

## 🧬 1. Lõi xử lý sinh học (Core Bioinformatics Engines)

### **FastQC**

* **Tác giả:** Simon Andrews, Babraham Bioinformatics
* **Tích hợp:** Mã nguồn Java của FastQC đã được **refactor** và tích hợp trực tiếp vào module phân tích chất lượng của SOAP
* **Giấy phép:** GPL v3

---

### **Trim Galore / Cutadapt** *(nếu có tích hợp)*

* **Tác giả:** Felix Krueger / Marcel Martin
* **Tích hợp:** SOAP hoạt động như một **wrapper** để thực thi Trim Galore trong bước làm sạch dữ liệu
* **Giấy phép:** GPL v3 / MIT

---

### **SPAdes** *(nếu có tích hợp)*

* **Tác giả:** Center for Algorithmic Biotechnology
* **Tích hợp:** Được sử dụng như một **external binary** cho quá trình lắp ráp hệ gen
* **Giấy phép:** GPL v2

---

### **Pharokka** *(nếu có tích hợp)*

* **Tác giả:** George Bouras
* **Tích hợp:** Được gọi thông qua **CLI wrapper** cho bước chú giải (annotation)
* **Giấy phép:** GPL v3

---

## 🛠️ 2. Thư viện Backend & Frontend

### **Spring Boot**

* **Mô tả:** Framework lõi cho Backend Java
* **Tác giả:** VMware / Spring.io
* **Giấy phép:** Apache License 2.0

---

### **React & Vite**

* **Mô tả:** Framework xây dựng giao diện người dùng (Frontend UI)
* **Tác giả:** Meta (Facebook) & cộng đồng mã nguồn mở
* **Giấy phép:** MIT License

---

### **D3.js**

* **Mô tả:** Thư viện trực quan hóa dữ liệu (data visualization) tương tác cao
* **Tác giả:** Mike Bostock
* **Giấy phép:** ISC License

---

## ⚖️ Tuân thủ giấy phép

Mọi thay đổi, bổ sung và tái cấu trúc mã nguồn trong dự án SOAP đều tuân thủ nghiêm ngặt các điều khoản của **GNU General Public License v3.0 (GPL-3.0)**.

Nếu có bất kỳ thiếu sót nào trong việc ghi nhận đóng góp hoặc giấy phép, vui lòng:

* Mở **Issue** trên repository
* Hoặc liên hệ trực tiếp với nhóm phát triển

👉 Chúng tôi sẽ cập nhật và khắc phục trong thời gian sớm nhất.
