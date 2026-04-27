package vn.dzokha.soap.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope; // Nên thêm Scope

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import java.util.List;
import java.util.ArrayList;

import vn.dzokha.soap.config.SOAPProperties;
import vn.dzokha.soap.domain.sequence.Sequence;
import vn.dzokha.soap.dto.response.chart.PointDTO;
import vn.dzokha.soap.engine.qc.core.QCModule;
import vn.dzokha.soap.engine.qc.core.ModuleConfig;
import vn.dzokha.soap.engine.qc.util.BaseGroup;

@Component
@Scope("prototype") // Đảm bảo mỗi file xử lý có một instance riêng
public class NContent implements QCModule {

	private final ModuleConfig moduleConfig;
    private final SOAPProperties properties; // 2. THÊM BIẾN PROPERTIES

    @Autowired
    public NContent(ModuleConfig moduleConfig, SOAPProperties properties) {
        this.moduleConfig = moduleConfig;
        this.properties = properties; // 3. INJECT VÀO CONSTRUCTOR
    }

	public long [] nCounts = new long [0];
	public long [] notNCounts = new long [0];
	public boolean calculated = false;
	public double [] percentages = null;
	public String [] xCategories = new String[0];

    @Override
    public Object getResultsPanel() {
        return null;
    }

    /**
     * Cung cấp dữ liệu thô cho Frontend qua REST API
     */
    public List<PointDTO> getChartData() {
        if (!calculated) getPercentages();
        List<PointDTO> data = new ArrayList<>();
        if (percentages == null) return data;
        
        for (int i = 0; i < xCategories.length; i++) {
            data.add(new PointDTO(xCategories[i], percentages[i]));
        }
        return data;
    }

	public boolean ignoreFilteredSequences() {
		return true;
	}
	
	public boolean ignoreInReport () {
		if (moduleConfig.getParam("n_content", "ignore") > 0) {
			return true;
		}
		return false;
	}
	
	private synchronized void getPercentages () {

		if (nCounts.length == 0) {
            calculated = true;
            return;
        }
        
        // 4. SỬA DÒNG 56: Truyền thêm this.properties
        BaseGroup [] groups = BaseGroup.makeBaseGroups(nCounts.length, this.properties);
        
        xCategories = new String[groups.length];
        percentages = new double [groups.length];

		long total;
		long nCount;

		for (int i=0;i<groups.length;i++) {
						
			xCategories[i] = groups[i].toString();

			nCount = 0;
			total = 0;
			
			for (int bp=groups[i].lowerCount()-1;bp<groups[i].upperCount();bp++) {		
				nCount += nCounts[bp];
				total += nCounts[bp];
				total += notNCounts[bp];
			}
			
			percentages[i] = 100*(nCount/(double)total);
		}
				
		calculated = true;
		
	}
		
	public void processSequence(Sequence sequence) {
		calculated = false;
		char [] seq = sequence.getSequence().toCharArray();
		if (nCounts.length < seq.length) {
			// We need to expand the size of the data structures
			
			long [] nCountsNew = new long [seq.length];
			long [] notNCountsNew = new long [seq.length];

			for (int i=0;i<nCounts.length;i++) {
				nCountsNew[i] = nCounts[i];
				notNCountsNew[i] = notNCounts[i];
			}
			
			nCounts = nCountsNew;
			notNCounts = notNCountsNew;
		}
		
		for (int i=0;i<seq.length;i++) {
			if (seq[i] == 'N') {
				++nCounts[i];
			}
			else {
				++notNCounts[i];
			}
		}
		
	}
	
	
	public String name() { return "Per base N content"; }
    public String description() { return "Shows the percentage of bases at each position which are not being called"; }
    
    public boolean raisesError() { 
        if (!calculated) getPercentages();
        // Bạn có thể thêm logic kiểm tra error từ moduleConfig nếu cần
        return false; 
    }
    
    public boolean raisesWarning() { 
        if (!calculated) getPercentages();
        // Bạn có thể thêm logic kiểm tra warning từ moduleConfig nếu cần
        return false; 
    }
    public void reset() { 
    	nCounts = new long[0]; 
    	notNCounts = new long[0]; 
    	calculated = false; 
    }

}
