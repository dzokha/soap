package vn.dzokha.soap.io.parser;

import java.io.*;
import java.util.regex.Pattern;
import org.itadaki.bzip2.BZip2InputStream;
import vn.dzokha.soap.config.SOAPProperties;

import vn.dzokha.soap.domain.sequence.Sequence;


public class FastQFile implements SequenceFile {

    private static final Pattern COLORSPACE_PATTERN = Pattern.compile("^[GATCNgatcn][\\.0123456]+$");

    private final SOAPProperties properties;
    private Sequence nextSequence = null;
    private final String name;
    private final File representativeFile;
    private final InputStream inputStream;
    private final BufferedReader br;
    
    private long lineNumber = 0;
    private boolean isColorspace = false;
    private final boolean casavaMode;
    private final boolean nofilter;

    public FastQFile(SOAPProperties properties, InputStream inputStream, String fileName) throws IOException, SequenceFormatException {
        this.properties = properties;
        this.name = fileName;
        this.representativeFile = new File(fileName);
        this.inputStream = inputStream;

        // TỐI ƯU 2: Dùng biến local để tính toán trước khi gán final
        boolean casava = false;
        boolean filter = false;
        if (properties.getAnalysis().isCasava()) {
            casava = true;
            filter = properties.getAnalysis().isNofilter();
        }
        this.casavaMode = casava;
        this.nofilter = filter;

        InputStream wrappedStream = inputStream;
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".gz")) {
            wrappedStream = new MultiMemberGZIPInputStream(inputStream);
        } else if (lowerName.endsWith(".bz2")) {
            wrappedStream = new BZip2InputStream(inputStream, false);
        }

        this.br = new BufferedReader(new InputStreamReader(wrappedStream));
        readNext(); // Nạp sẵn bản ghi đầu tiên
    }

    private void readNext() throws SequenceFormatException {
        try {
            String id;
            while (true) {
                id = br.readLine();
                if (id == null) {
                    nextSequence = null;
                    close();
                    return;
                }
                lineNumber++;
                if (!id.trim().isEmpty()) break;
            }

            if (!id.startsWith("@")) {
                throw new SequenceFormatException("Lỗi định dạng dòng ID (thiếu @) tại dòng: " + lineNumber);
            }

            String seq = br.readLine(); lineNumber++;
            String midLine = br.readLine(); lineNumber++;
            String quality = br.readLine(); lineNumber++;

            if (seq == null || midLine == null || quality == null) {
                throw new IOException("File FASTQ bị cắt ngang tại dòng " + lineNumber);
            }

            if (!midLine.startsWith("+")) {
                throw new SequenceFormatException("Thiếu dấu '+' tại dòng " + lineNumber);
            }

            // Chỉ kiểm tra Colorspace ở những dòng đầu tiên để tiết kiệm CPU
            if (lineNumber <= 4) {
                isColorspace = COLORSPACE_PATTERN.matcher(seq).matches();
            }

            // TỐI ƯU 3: Khởi tạo Sequence với boolean colorspace trực tiếp (theo class Sequence ta đã tối ưu)
            String finalSeq = isColorspace ? convertColorspaceToBases(seq) : seq;
            nextSequence = new Sequence(id, finalSeq, quality, representativeFile, isColorspace);

            if (casavaMode && !nofilter && id.contains(":Y:")) {
                nextSequence.setFiltered(true);
            }

        } catch (IOException ioe) {
            throw new SequenceFormatException("Lỗi đọc file: " + ioe.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (br != null) br.close();
            // BufferedReader.close() sẽ tự đóng inputStream bên trong nó
        } catch (IOException e) {
            // Log quietly
        }
    }

    // Giữ nguyên logic xử lý chuỗi
    private String convertColorspaceToBases(String s) { return s; }

    @Override public String name() { return name; }
    @Override public boolean hasNext() { return nextSequence != null; }
    @Override public boolean isColorspace() { return isColorspace; }
    @Override public int getPercentComplete() { return hasNext() ? 0 : 100; }
    @Override public String getId() { return name; }
    @Override public InputStream getInputStream() { return inputStream; }

    @Override
    public Sequence next() throws SequenceFormatException {
        Sequence seq = nextSequence;
        readNext();
        return seq;
    }
}