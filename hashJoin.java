import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import com.opencsv.*;
import com.google.common.collect.*;

/**
 * A class that implement double-pipelined hash join with multi-core support.
 * @author Haoran Sun
 * @since 01/14/2017
 */
public class hashJoin {
	static final int SIZE = 2003;
	static final int FREQ_CAP = 35;
	static final int CHN_IDX = 2;
	static final int OUTPUT_IDX = 2;
	static final String FMT = "UTF-8";
	
	//private Multimap dict;
	private HashMultimap<String, String> dict;
	private Hashtable<String, String> doc;
	private PrintWriter writer;
	
	/**
	 * Default constructor
	 */
	public hashJoin() {
		//this.dict = new Multimap();
		this.dict = HashMultimap.create();
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
		if(option == 0) { //Probe the dictionary and insert to document
			Set<String> engWords = this.dict.get(key);
			Iterator<String> it = engWords.iterator();
			String w;
			while(it.hasNext()) {
				w = it.next();
				writer.println("<" + w + ">" + key + "</" + w + ">");
			}
			this.doc.put(key, word);
		}else { //Probe the document and insert to the dictionary
			if(this.doc.get(key) != null)
				writer.println("<" + word + ">" + key + "</" + word +
						">");
			this.dict.put(key, word);
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
			while(sc.hasNext()) { //Double-pipelined hash join
				if(readFromDict){
					if((dictLine = reader.readNext()) == null) break;
					
					if(dictLine[1].length() == 0) //Case 1: nominal term
						this.probeAndInsert(dictLine[CHN_IDX], dictLine[0], 1);
					else //Case 2: whole sentence
						this.probeAndInsert(dictLine[CHN_IDX], 
								dictLine[1].replace("~", dictLine[0]), 1);
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
				this.dict.put(dictLine[CHN_IDX], dictLine[0]);
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
	
	public void loadDict(String dict) {
		try {
			InputStreamReader in = new InputStreamReader(new FileInputStream(dict), FMT);
			CSVReader reader = new CSVReader(in);
			int[] freq = new int[FREQ_CAP];
			String[] dictLine;
			while((dictLine = reader.readNext()) != null) {
				this.dict.put(dictLine[CHN_IDX], dictLine[0]);
				freq[dictLine[CHN_IDX].length()]++;
			}
			reader.close();
			printHistogram(freq);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printHistogram(int[] arr) {
		for(int i = 1; i < arr.length; i++) {
			System.out.println(i + ": " + arr[i]);
		}
	}
	
	/**
	 * Main method to run hash join
	 * @param args
	 */
	public static void main(String[] args) {
		hashJoin hj = new hashJoin();
		hj.loadDict(args[0]);
	}
}