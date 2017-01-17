import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Scanner;

import com.opencsv.*;

/**
 * A class that implement double-pipelined hash join with multi-core support.
 * @author Haoran Sun
 * @since 01/14/2017
 */
public class hashJoin {
	static final int SIZE = 2003;
	static final int CHN_IDX = 2;
	static final int OUTPUT_IDX = 2;
	static final String FMT = "UTF-8";
	
	private Multimap dict;
	private Hashtable<String, String> doc;
	private PrintWriter writer;
	
	/**
	 * Default constructor
	 */
	public hashJoin() {
		this.dict = new Multimap();
		this.doc = new Hashtable<String, String>(SIZE);
	}
	
	/**
	 * Helper method to probe and insert data
	 * @param key Chinese word
	 * @param word English word
	 * @param option 0 to insert to the document hashtable; 1 to insert to the
	 * dictioanry hashtable
	 */
	private void probeAndInsert(String key, String word, int option) {
		if(option == 0) {
			LinkedList<String> queryResult = this.dict.get(key);
			if(queryResult != null) {
				for(int i = 0; i < queryResult.size(); i++) {
					writer.println("<" + queryResult.get(i) + ">" + key + 
							"</" + queryResult.get(i) + ">");
				}
			}
			this.doc.put(key, word);
		}else {
			this.dict.insert(key, word);
			if(this.doc.get(key) != null) {
				LinkedList<String> queryResult = this.dict.get(key);
				for(int i = 0; i < queryResult.size(); i++) {
					writer.println("<" + queryResult.get(i) + ">" + key + 
							"</" + queryResult.get(i) + ">");
				}
			}
		}
	}
	
	/**
	 * Double-pipelined hash join
	 * @param dict dictionary filename
	 * @param doc document filename
	 * @param out output filename
	 */
	public void doublePipelinedHashJoin(String dict, String doc, String out) {
		boolean readFromDict = true;
		try {
			this.writer = new PrintWriter(new OutputStreamWriter
					(new FileOutputStream(out), FMT));
			InputStreamReader in = new InputStreamReader(new FileInputStream(dict), FMT);
			CSVReader reader = new CSVReader(in);
			String[] dictLine;
			Scanner sc = new Scanner(new FileInputStream(doc), FMT);
			String docWord;
			while(sc.hasNext()) {
				if(readFromDict){
					if((dictLine = reader.readNext()) == null) break;
					this.probeAndInsert(dictLine[CHN_IDX], dictLine[0], 1);
					readFromDict = false;
				}else {
					docWord = sc.next();
					if(docWord.contains("/n")) {
						docWord = docWord.substring(0, docWord.indexOf('/'));
						if(doc.length() != 0)
							this.probeAndInsert(docWord, docWord, 0);
					}
					readFromDict = true;
				}
			}
			while((dictLine = reader.readNext()) != null) {
				this.probeAndInsert(dictLine[CHN_IDX], dictLine[0], 1);
			}
			reader.close();
			while(sc.hasNext()) {
				docWord = sc.next();
				if(docWord.contains("/n")) {
					docWord = docWord.substring(0, docWord.indexOf('/'));
					if(doc.length() != 0)
						this.probeAndInsert(docWord, docWord, 0);
				}
			}
			sc.close();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Simple hash join method; for testing purpose only
	 * @param dict dictionary filename
	 * @param doc document filename
	 * @param out output filename
	 */
	public void simpleHashJoin (String dict, String doc, String out) {
		try {
			this.writer = new PrintWriter(new OutputStreamWriter
					(new FileOutputStream(out), FMT));
			InputStreamReader in = new InputStreamReader(new FileInputStream(dict), FMT);
			CSVReader reader = new CSVReader(in);
			String[] dictLine;
			Scanner sc = new Scanner(new FileInputStream(doc), FMT);
			String docWord;
			while((dictLine = reader.readNext()) != null) {
				this.dict.insert(dictLine[CHN_IDX], dictLine[0]);
			}
			reader.close();
			while(sc.hasNext()) {
				docWord = sc.next();
				if(docWord.contains("/n")) {
					docWord = docWord.substring(0, docWord.indexOf('/'));
					if(doc.length() != 0)
						this.probeAndInsert(docWord, docWord, 0);
				}
			}
			sc.close();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Main method to run hash join
	 * @param args
	 */
	public static void main(String[] args) {
		hashJoin hj = new hashJoin();
		hj.doublePipelinedHashJoin(args[0], args[1], "output_pipelined.txt");
		//hj.simpleHashJoin(args[0], args[1], args[OUTPUT_IDX]);
	}
}