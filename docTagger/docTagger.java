/**
 * Document tagger and its driver.
 * @author Haoran Sun
 * @since 01/26/2017
 */
package docTagger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.*;
import com.opencsv.*;

/**
 * A class that implement a document tagger (tag Chinese words with their
 * English meaning) using a hash multi-map and self-balanced interval search
 * tree.
 *  
 * @author Haoran Sun
 * @since 01/26/2017
 */
public class docTagger {
	static final int CHN_IDX = 2;
	static final int OUTPUT_IDX = 2;
	static final int FREQ_CAP = 35; //The size of frequency array
	static final String FMT = "UTF-8";
	static final String TAG = "<%s>%s</%s>"; //Tag formatter
	
	private HashMultimap<String, String> dict;
	private int[] freq; //Array that stores the frequency of word lengths
	
	/**
	 * Constructor
	 */
	public docTagger() {
		this.dict = HashMultimap.create();
		this.freq = new int[FREQ_CAP];
	}
	
	/**
	 * Load the whole dictionary to a hash multi-map
	 * @param dictName the dictionary file name
	 */
	public void loadDict(String dictName) {
		InputStreamReader in;
		CSVReader reader;
		try { /* Read and hash to the multi-map */
			in = new InputStreamReader(new FileInputStream(dictName), FMT);
			reader = new CSVReader(in);
			String[] dictLine;
			/* Use Chinese words as keys and English words as values */
			while((dictLine = reader.readNext()) != null) {
				if(dictLine[1].contains("~"))
					dictLine[0] = dictLine[1].replace("~", dictLine[0]);
				this.dict.put(dictLine[CHN_IDX], dictLine[0]);
				/* Update frequency in terms of word lengths */
				freq[dictLine[CHN_IDX].length()]++;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Print the distribution of word lengths of the dictionary
	 */
	public void getStats() {
		for(int i = 1; i < freq.length; i++) {
			System.out.println(i + ": " + freq[i]);
		}
	}
	
	/**
	 * Read the document and output tagged text using n-gram matching
	 * @param docName filename of the input document
	 * @param outFile output filename
	 * @param n the length of the longest word to match
	 */
	public void process(String docName, String outFile, int n) {
		try {
			Scanner sc = new Scanner(new FileInputStream(docName), FMT);
			/*
			 * Note: still need to think about how to process segments
			 * May be need to split by "," as well
			 */
			String[] content = sc.next().split("¡£");
			sc.close();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter
					(new FileOutputStream(outFile), FMT));
			int wordLength = n;
			
			/* Iterate through all sentences */
			for(int i = 0; i < content.length; i++) {
				IntervalTree<IndexInterval> ist = new IntervalTree<>();
				int endIndex = 0;
				wordLength = n;
				
				/* First round word matching using the word length n */
				for(int j = 0; j < content[i].length();) {
					endIndex = j + wordLength - 1;
					if(endIndex >= content[i].length()) break;
					
					/* Word matching */
					if(this.dict.containsKey(content[i].substring(j,
							endIndex + 1))) {
						/* Mark interval */
						ist.insert(new IndexInterval(j, endIndex));
						j = endIndex + 1; //Get next index
					}else {
						j++;
					}
				}
				wordLength--; //Ready to match word with shorter length
				
				int nextPos;
				for(; wordLength > 1; wordLength--) { //Try matching all words until the length 2
					/* Start from the first available position */
					for(int j = ist.nextAvailable(new IndexInterval(0, wordLength - 1));
							j < content[i].length();) {
						endIndex = j + wordLength - 1;
						if(endIndex >= content[i].length()) break;
						
						nextPos = ist.nextAvailable(new IndexInterval(j,
								endIndex)); //Check overlap
						if(j == nextPos) { //No overlap found
							/* Word matching */
							if(this.dict.containsKey(content[i].substring(j,
									endIndex + 1))) {
								/* Mark interval */
								ist.insert(new IndexInterval(j, endIndex));
								j = endIndex + 1; //Get next index
							}else {
								j++;
							}
						}else
							j = nextPos; //Try next available position
					}
				}
				
				/* Start output */
				Set<String> matchWords;
				String enWord;
				String cnWord;
				for(int j = 0; j < content[i].length();) {
					nextPos = ist.nextAvailable(new IndexInterval(j, j));
					if(nextPos == j) { //Single character is not matched yet
						matchWords = this.dict.get(content[i].charAt(j) + "");
						/* No match found */
						if(matchWords.isEmpty())
							writer.print(content[i].charAt(j));
						else {
							/* Get the first match */
							enWord = (String) matchWords.toArray()[0];
							writer.print(String.format(TAG, enWord,
									content[i].charAt(j), enWord));
						}
						j++;
					}else {
						/* Obtain the Chinese word */
						cnWord = content[i].substring(j, nextPos);
						matchWords = this.dict.get(cnWord);
						enWord = (String) matchWords.toArray()[0];
						writer.print(String.format(TAG, enWord, cnWord,
								enWord));
						j = nextPos; //Skip to the start of the next word
					}
				}
				writer.print("¡£");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Driver of the document tagger
	 * @param args command line arguments
	 */
	public static void main(String args[]) {
		docTagger tagger = new docTagger();
		tagger.loadDict(args[0]);
		System.out.println("Dictionary loaded.");
		tagger.getStats();
		tagger.process(args[1], args[OUTPUT_IDX], 12);
	}
}