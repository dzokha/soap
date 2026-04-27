package vn.dzokha.soap.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Component; // Đảm bảo đã có cái này nếu dùng @Component
import org.springframework.beans.factory.annotation.Autowired;

import vn.dzokha.soap.config.SOAPProperties;
import vn.dzokha.soap.domain.sequence.Sequence;
import vn.dzokha.soap.domain.sequence.ContaminantHit;
import vn.dzokha.soap.domain.sequence.ContaminentFinder;
import vn.dzokha.soap.engine.qc.core.QCModule;
import vn.dzokha.soap.engine.qc.core.ModuleConfig;

@Component
public class OverRepresentedSeqs implements QCModule {

	private final ModuleConfig moduleConfig;
    private final SOAPProperties properties;
    private final ContaminentFinder contaminentFinder;
    
    // SỬA LỖI: Chỉ dùng một Constructor duy nhất và đúng tên Class
    @Autowired
    public OverRepresentedSeqs(ModuleConfig moduleConfig, 
                               SOAPProperties properties, 
                               ContaminentFinder contaminentFinder) {
        this.moduleConfig = moduleConfig;
        this.properties = properties;
        this.contaminentFinder = contaminentFinder;
		this.duplicationModule = new DuplicationLevel(moduleConfig);
    }

	protected HashMap<String, Integer>sequences = new HashMap<String, Integer>();
	protected long count = 0;
	private OverrepresentedSeq [] overrepresntedSeqs = null;
	private boolean calculated = false;
	private boolean frozen = false;
	private DuplicationLevel duplicationModule;
	
	// This is the number of different sequences we want to track
	private final int OBSERVATION_CUTOFF = 100000;
	// This is a count of how many unique sequences we've seen so far
	// so we know when to stop adding them.
	private int uniqueSequenceCount = 0;
	// This was the total count at the point at which we saw our total
	// number of unique sequences, so we know what to correct by when
	// extrapolating to the whole file
	protected long countAtUniqueLimit = 0;

	public void setDuplicationModule(DuplicationLevel duplicationModule) {
        this.duplicationModule = duplicationModule;
    }
	
	public boolean ignoreInReport () {
		if (moduleConfig.getParam("overrepresented", "ignore") > 0) {
			return true;
		}
		return false;
	}
	
	public String description() {
		return "Identifies sequences which are overrepresented in the set";
	}
	
	public boolean ignoreFilteredSequences() {
		return true;
	}
	
	public DuplicationLevel duplicationLevelModule () {
		return duplicationModule;
	}

	public JPanel getResultsPanel() {
		JPanel returnPanel = new JPanel();
		returnPanel.setLayout(new BorderLayout());
		returnPanel.add(new JLabel("Overrepresented sequences",JLabel.CENTER),BorderLayout.NORTH);
		
		if (!calculated) getOverrepresentedSeqs();
		
		if (overrepresntedSeqs.length > 0) {
			TableModel model = new ResultsTable(overrepresntedSeqs);
			JTable table = new JTable(model);
			table.setCellSelectionEnabled(true);
			returnPanel.add(new JScrollPane(table),BorderLayout.CENTER);
		}
		else {
			returnPanel.add(new JLabel("There are no overrepresented sequences",JLabel.CENTER),BorderLayout.CENTER);
		}
		
		return returnPanel;
	
	}
	
	public DuplicationLevel getDuplicationLevelModule () {
		return duplicationModule;
	}
	private synchronized void getOverrepresentedSeqs () {

		if (calculated) return;

        // Cần kiểm tra null trước khi gọi duplicationModule
        if (duplicationModule != null) {
            duplicationModule.calculateLevels();
        }
		
		Iterator<String> s = sequences.keySet().iterator();
		List<OverrepresentedSeq>keepers = new ArrayList<OverrepresentedSeq>();
		
		while (s.hasNext()) {
			String seq = s.next();
			double percentage = ((double)sequences.get(seq)/count)*100;
			if (percentage > moduleConfig.getParam("overrepresented", "warn")) {
				OverrepresentedSeq os = new OverrepresentedSeq(seq, sequences.get(seq), percentage);
				keepers.add(os);
			}
		}
		
		overrepresntedSeqs = keepers.toArray(new OverrepresentedSeq[0]);
		Arrays.sort(overrepresntedSeqs);
		calculated  = true;
		sequences.clear();
		
	}
	
	public void reset () {
		count = 0;
		sequences.clear();
	}

	public String name() {
		return "Overrepresented sequences";
	}

	public void processSequence(Sequence sequence) {
		calculated = false;
		++count;
		
		String seq = sequence.getSequence();

		// THAY THẾ: Sử dụng properties thay vì SOAPConfig.getInstance()
		int dupLength = properties.getAnalysis().getDupLength();
		
		if (dupLength != 0) {
            // Đảm bảo không bị lỗi IndexOutOfBounds nếu sequence ngắn hơn dupLength
            int end = Math.min(seq.length(), dupLength);
			seq = seq.substring(0, end);			
		}
		else if (seq.length() > 50) {
			seq = seq.substring(0, 50);
		}
				
		if (sequences.containsKey(seq)) {
			sequences.put(seq, sequences.get(seq)+1);
			if (!frozen) {
				countAtUniqueLimit = count;
			}
		}
		else {
			if (!frozen) {
				sequences.put(seq, 1);
				++uniqueSequenceCount;
				countAtUniqueLimit = count;
				if (uniqueSequenceCount == OBSERVATION_CUTOFF) {
					frozen = true;
				}
			}
		}		
	}

		
	
	@SuppressWarnings("serial")
	private class ResultsTable extends AbstractTableModel {
		
		private OverrepresentedSeq [] seqs;
		
		public ResultsTable (OverrepresentedSeq [] seqs) {
			this.seqs = seqs;
		}
		
		
		// Sequence - Count - Percentage
		public int getColumnCount() {
			return 4;
		}

		public int getRowCount() {
			return seqs.length;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0: return seqs[rowIndex].seq();
				case 1: return seqs[rowIndex].count();
				case 2: return seqs[rowIndex].percentage();
				case 3: return seqs[rowIndex].contaminantHit();
					
			}
			return null;
		}
		
		public String getColumnName (int columnIndex) {
			switch (columnIndex) {
				case 0: return "Sequence";
				case 1: return "Count";
				case 2: return "Percentage";
				case 3: return "Possible Source";
			}
			return null;
		}
		
		public Class<?> getColumnClass (int columnIndex) {
			switch (columnIndex) {
			case 0: return String.class;
			case 1: return Integer.class;
			case 2: return Double.class;
			case 3: return String.class;
		}
		return null;
			
		}
	}

	
	private class OverrepresentedSeq implements Comparable<OverrepresentedSeq>{
		
		private String seq;
		private int count;
		private double percentage;
		private ContaminantHit contaminantHit;
		
		public OverrepresentedSeq (String seq, int count, double percentage) {
			this.seq = seq;
			this.count = count;
			this.percentage = percentage;
			this.contaminantHit = contaminentFinder.findContaminantHit(seq);

		}
		
		public String seq () {
			return seq;
		}
		
		public int count () {
			return count;
		}
		
		public double percentage () {
			return percentage;
		}

		public String contaminantHit () {
			return (contaminantHit == null) ? "No Hit" : contaminantHit.toString();
		}

		public int compareTo(OverrepresentedSeq o) {
			return o.count - this.count;
		}
	}

	public boolean raisesError() {
		if (!calculated) getOverrepresentedSeqs();
		if (overrepresntedSeqs.length>0) {
			if (overrepresntedSeqs[0].percentage > moduleConfig.getParam("overrepresented", "error")) {
				return true;
			}
		}
		return false;
	}

	public boolean raisesWarning() {
		if (!calculated) getOverrepresentedSeqs();

		if (overrepresntedSeqs.length > 0) return true;
		return false;
	}

}
