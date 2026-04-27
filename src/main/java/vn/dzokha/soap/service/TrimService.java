package com.soap.pipeline.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrimService {

    public String runTrim(String inputFile, String outputFile, boolean runFastQC) {
        try {
            // 🔧 1. Build command Cutadapt
            List<String> command = new ArrayList<>();
            command.add("cutadapt");
            command.add("-a");
            command.add("AGATCGGAAGAGC"); // default adapter (Illumina)
            command.add("-q");
            command.add("20"); // quality cutoff
            command.add("-o");
            command.add(outputFile);
            command.add(inputFile);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 📄 đọc log output
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Cutadapt failed:\n" + log);
            }

            // 📊 2. Optional: chạy FastQC
            if (runFastQC) {
                runFastQC(outputFile);
            }

            return log.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error running Trim pipeline", e);
        }
    }

    private void runFastQC(String file) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("fastqc", file);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
    }
}