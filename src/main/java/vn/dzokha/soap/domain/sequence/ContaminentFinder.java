/**
 * Copyright Copyright 2010-17 Simon Andrews
 *
 *    This file is part of soap.
 *
 *    SOAP is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SOAP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SOAP; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package vn.dzokha.soap.domain.sequence;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;


import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import vn.dzokha.soap.config.SOAPProperties;

@Component
public class ContaminentFinder {

	private final SOAPProperties properties;
	private final ResourceLoader resourceLoader;

	private static Contaminant [] contaminants;

	public ContaminentFinder(SOAPProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }
	

	/**
     * Tìm kiếm trình tự tạp nhiễm tốt nhất cho một chuỗi đầu vào.
     * Lưu ý: Không còn là static để có thể truy cập properties của Spring Bean.
     */
    public ContaminantHit findContaminantHit(String sequence) {
        if (contaminants == null) {
            contaminants = makeContaminantList();
        }

        ContaminantHit bestHit = null;

        for (int c = 0; c < contaminants.length; c++) {
            ContaminantHit thisHit = contaminants[c].findMatch(sequence);

            if (thisHit == null) continue;

            if (bestHit == null || thisHit.length() > bestHit.length() 
                || (thisHit.length() == bestHit.length() && thisHit.percentID() > bestHit.percentID())) {
                bestHit = thisHit;
            }
        }

        return bestHit;
    }

    /**
     * Khởi tạo danh sách các trình tự tạp nhiễm từ file cấu hình.
     */
    private Contaminant[] makeContaminantList() {
        Vector<Contaminant> c = new Vector<Contaminant>();

        try {
            BufferedReader br = null;
            String configPath = properties.getAnalysis().getContaminantFile();

            // Sử dụng ResourceLoader để xử lý cả đường dẫn classpath: và file:
            if (configPath == null || configPath.isEmpty()) {
                // Fallback mặc định nếu không có cấu hình (theo cấu trúc folder cũ hoặc mặc định dự phòng)
                InputStream rsrc = ContaminentFinder.class.getResourceAsStream("/data/contaminant_list.txt");
                if (rsrc == null) throw new FileNotFoundException("Cannot find default /data/contaminant_list.txt");
                br = new BufferedReader(new InputStreamReader(rsrc));
            } else {
                Resource resource = resourceLoader.getResource(configPath);
                br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue; 
                if (line.trim().length() == 0) continue; 

                String[] sections = line.split("\\t+");
                if (sections.length != 2) {
                    System.err.println("Expected 2 sections for contaminant line but got " + sections.length + " from " + line);
                    continue;
                }
                Contaminant con = new Contaminant(sections[0], sections[1]);
                c.add(con);
            }

            br.close();
        } catch (IOException e) {
            System.err.println("Error loading contaminants: " + e.getMessage());
        }

        return c.toArray(new Contaminant[0]);
    }
	
	/*
	public static void main (String [] args) {
		
		Config cfg=new Config();
		String query = "agagtgtagatctccgtggtcgccgtatca";
		
		ContaminantHit c = findContaminantHit(cfg,query);
		
		System.out.println("Query was "+query.length()+"bp Found hit "+c);
		
	}*/
	
}
