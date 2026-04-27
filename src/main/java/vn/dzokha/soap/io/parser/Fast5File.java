package vn.dzokha.soap.io.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

import vn.dzokha.soap.domain.sequence.Sequence;


public class Fast5File implements SequenceFile {

    private final String name;
    private final File tempFile;
    private final IHDF5SimpleReader reader;
    private final InputStream inputStream; // THÊM DÒNG NÀY
    
    private String[] readPaths = new String[] {""};
    private int readPathsIndexPosition = 0;
    
    private final String[] rdfPaths = new String[] {
            "Analyses/Basecall_2D_000/BaseCalled_template/Fastq",
            "Analyses/Basecall_2D_000/BaseCalled_2D/Fastq",
            "Analyses/Basecall_1D_000/BaseCalled_template/Fastq",
            "Analyses/Basecall_1D_000/BaseCalled_1D/Fastq"
    };

    /**
     * Constructor thuần Web: Nhận InputStream và xử lý qua file tạm
     */
    public Fast5File(InputStream inputStream, String name) throws SequenceFormatException, IOException {
        this.name = name;
        this.inputStream = inputStream; // GÁN GIÁ TRỊ TẠI ĐÂY
        
        // Tạo file tạm vì HDF5 yêu cầu Random Access
        this.tempFile = File.createTempFile("SOAP_upload_", ".fast5");
        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        this.reader = HDF5Factory.openForReading(tempFile);
        
        initializeReadPaths();
    }

    private void initializeReadPaths() {
        List<String> topLevelFolders = reader.getGroupMembers("/");
        List<String> readFolders = new ArrayList<>();
        
        for (String folder : topLevelFolders) {
            if (folder.startsWith("read_")) {
                readFolders.add(folder + "/");
            }
        }
        
        if (!readFolders.isEmpty()) {
            readPaths = readFolders.toArray(new String[0]);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int getPercentComplete() {
        if (readPaths.length == 0) return 100;
        return (readPathsIndexPosition * 100) / readPaths.length;        
    }

    @Override
    public boolean isColorspace() {
        return false;
    }

    @Override
    public boolean hasNext() {
        return readPathsIndexPosition < readPaths.length;
    }

    @Override
    public Sequence next() throws SequenceFormatException {
        for (String rdfPath : rdfPaths) {
            String fullPath = readPaths[readPathsIndexPosition] + rdfPath;
            
            if (reader.exists(fullPath)) {
                String fastq = reader.readString(fullPath);
                String[] sections = fastq.split("\\n");
        
                if (sections.length < 4) {
                    throw new SequenceFormatException("Invalid Fastq structure in HDF5 path: " + fullPath);
                }
        
                // Tạo Sequence thuần data (không truyền 'this')
                Sequence seq = new Sequence(sections[0], sections[1].toUpperCase(), sections[3]);
                
                readPathsIndexPosition++;
                if (readPathsIndexPosition >= readPaths.length) {
                    close();
                }
                
                return seq;
            }
        }
        
        close();
        throw new SequenceFormatException("No valid fastq paths found in " + name);
    }

    /**
     * Giải phóng tài nguyên và xóa file tạm ngay khi xử lý xong
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return this.inputStream;
    }

    @Override
    public String getId() {
        return this.name;
    }

    @Override
    public void close() {
        try {
            if (reader != null) reader.close();
            if (tempFile != null && tempFile.exists()) {
                java.nio.file.Files.deleteIfExists(tempFile.toPath());
            }
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            // Log error
        }
    }

}