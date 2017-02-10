/**
 * Document tagger and its driver.
 * @author Haoran Sun
 * @since 01/26/2017
 */
package docTagger;

import java.io.File;
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
 */
public class docTagger implements Runnable {
  static final int CHN_IDX = 2;
  static final int OUTPUT_IDX = 2;
  static final int FREQ_CAP = 35; // The size of frequency array
  static final int NUM_ARGS = 3;
  static final int LAW_LNG = 2;
  static final int INIT_WL = 12;
  static final String FMT = "UTF-8";
  static final String TAG = "<%s>%s</%s>"; // Tag formatter
  static final String OUT_PREFIX = "out";
  static final String OUT_PATH = "output/";
  static final String LAW_CHN = "·¨¡·";
  static final String LAW_CHN_Q = "·¨¡µ";
  static final String LAW_ENG = "-law";
  static final String IGNORE = ".-.-.";

  private HashMultimap<String, String> dict;
  //private int[] freq; // Array that stores the frequency of word lengths
  private String content;
  private int id;

  /**
   * Constructor
   * @param dict - dictionary
   * @param content - file content
   * @param id - process id
   */
  public docTagger(HashMultimap<String, String> dict, String content, int id) {
    this.dict = dict;
    this.content = content;
    this.id = id;
    //this.freq = new int[FREQ_CAP];
  }

  /**
   * Load the whole dictionary to a hash multi-map
   * 
   * @param dictName - the dictionary file name
   */
  public void loadDict(String dictName) {
    InputStreamReader in;
    CSVReader reader;
    try { /* Read and hash to the multi-map */
      in = new InputStreamReader(new FileInputStream(dictName), FMT);
      reader = new CSVReader(in);
      String[] dictLine;
      /* Use Chinese words as keys and English words as values */
      while ((dictLine = reader.readNext()) != null) {
        if (dictLine[1].contains("~"))
          dictLine[0] = dictLine[1].replace("~", dictLine[0]);
        if(dictLine[0].contains(" "))
          dictLine[0] = dictLine[0].replace(" ", "-");
        this.dict.put(dictLine[CHN_IDX], dictLine[0]);
        /* Update frequency in terms of word lengths */
        //freq[dictLine[CHN_IDX].length()]++;
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Print the distribution of word lengths of the dictionary
   */
  /*
  public void getStats() {
    for (int i = 1; i < freq.length; i++) {
      System.out.println(i + ": " + freq[i]);
    }
  }*/

  /**
   * Read the document and output tagged text using n-gram matching
   * 
   * @param docName - filename of the input document
   * @param outFile - output filename
   * @param n - the length of the longest word to match
   */
  public void process(String docName, String outFile, int n) {
    try {
      
      Scanner sc = new Scanner(new FileInputStream(docName), FMT);
      sc.useDelimiter("\\Z");
      /*
       * Note: still need to think about how to process segments May be need to
       * split by "," as well
       */
      String[] content = sc.next().split("¡£");
      sc.close();
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
          FileOutputStream(outFile), FMT));
      int wordLength = n;

      /* Iterate through all sentences */
      for (int i = 0; i < content.length; i++) {
        IntervalTree<IndexInterval> ist = new IntervalTree<>();
        int endIndex = 0;
        wordLength = n;

        /* First round word matching using the word length n */
        for (int j = 0; j < content[i].length();) {
          endIndex = j + wordLength - 1;
          if (endIndex >= content[i].length())
            break;

          /* Word matching */
          if (this.dict.containsKey(content[i].substring(j, endIndex + 1))) {
            /* Mark interval */
            ist.insert(new IndexInterval(j, endIndex));
            j = endIndex + 1; // Get next index
          } else {
            j++;
          }
        }
        wordLength--; // Ready to match word with shorter length

        int nextPos;
        for (; wordLength > 1; wordLength--) { // Try matching all words until
                                               // the length 2
          /* Start from the first available position */
          for (int j = ist.nextAvailable(new IndexInterval(0, wordLength - 1));
              j < content[i].length();) {
            endIndex = j + wordLength - 1;
            if (endIndex >= content[i].length())
              break;

            nextPos = ist.nextAvailable(new IndexInterval(j, endIndex)); // Check
                                                                         // overlap
            if (j == nextPos) { // No overlap found
              /* Word matching */
              if (this.dict.containsKey(content[i].substring(j, endIndex + 1))) {
                /* Mark interval */
                ist.insert(new IndexInterval(j, endIndex));
                j = endIndex + 1; // Get next index
              } else {
                j++;
              }
            } else
              j = nextPos; // Try next available position
          }
        }

        /* Start output */
        Set<String> matchWords;
        String enWord;
        String cnWord;
        for (int j = 0; j < content[i].length();) {
          nextPos = ist.nextAvailable(new IndexInterval(j, j));
          if (nextPos == j) { // Single character is not matched yet
            matchWords = this.dict.get(content[i].charAt(j) + "");
            /* No match found */
            if (matchWords.isEmpty())
              writer.print(content[i].charAt(j));
            else {
              /* Get the first match */
              enWord = (String) matchWords.toArray()[0];
              writer.print(String.format(TAG, enWord, content[i].charAt(j), enWord));
            }
            j++;
          } else {
            /* Obtain the Chinese word */
            cnWord = content[i].substring(j, nextPos);
            matchWords = this.dict.get(cnWord);
            /* Special case for law */
            if(nextPos + LAW_LNG < content[i].length() &&
                (content[i].substring(nextPos, nextPos +
                    LAW_LNG).equals(LAW_CHN) || content[i].substring(nextPos,
                        nextPos + LAW_LNG).equals(LAW_CHN_Q))) {
              cnWord = content[i].substring(j, nextPos + 1);
              enWord = (String) matchWords.toArray()[0] + LAW_ENG;
              nextPos++;
            }
            else
              enWord = (String) matchWords.toArray()[0];
            if(!enWord.contains(IGNORE))
              writer.print(String.format(TAG, enWord, cnWord, enWord));
            else
              writer.print(cnWord);
            j = nextPos; // Skip to the start of the next word
          }
        }
        if(i < content.length - 1) writer.print("¡£");
      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Read the document and output tagged text using n-gram matching with multi-
   * threading
   */
  public void run() {
    try {
      /*
       * Note: still need to think about how to process segments May be need to
       * split by "," as well
       */
      String[] content = this.content.split("¡£");
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
          FileOutputStream(OUT_PATH + OUT_PREFIX + id + ""), FMT));
      int wordLength = INIT_WL;

      /* Iterate through all sentences */
      for (int i = 0; i < content.length; i++) {
        IntervalTree<IndexInterval> ist = new IntervalTree<>();
        int endIndex = 0;
        wordLength = INIT_WL;

        /* First round word matching using the word length n */
        for (int j = 0; j < content[i].length();) {
          endIndex = j + wordLength - 1;
          if (endIndex >= content[i].length())
            break;

          /* Word matching */
          if (this.dict.containsKey(content[i].substring(j, endIndex + 1))) {
            /* Mark interval */
            ist.insert(new IndexInterval(j, endIndex));
            j = endIndex + 1; // Get next index
          } else {
            j++;
          }
        }
        wordLength--; // Ready to match word with shorter length

        int nextPos;
        for (; wordLength > 1; wordLength--) { // Try matching all words until the length 2
          /* Start from the first available position */
          for (int j = ist.nextAvailable(new IndexInterval(0, wordLength - 1));
              j < content[i].length();) {
            endIndex = j + wordLength - 1;
            if (endIndex >= content[i].length())
              break;

            nextPos = ist.nextAvailable(new IndexInterval(j, endIndex)); // Check overlap
            if (j == nextPos) { // No overlap found
              /* Word matching */
              if (this.dict.containsKey(content[i].substring(j, endIndex + 1))) {
                /* Mark interval */
                ist.insert(new IndexInterval(j, endIndex));
                j = endIndex + 1; // Get next index
              } else {
                j++;
              }
            } else
              j = nextPos; // Try next available position
          }
        }

        /* Start output */
        Set<String> matchWords;
        String enWord;
        String cnWord;
        for (int j = 0; j < content[i].length();) {
          nextPos = ist.nextAvailable(new IndexInterval(j, j));
          if (nextPos == j) { // Single character is not matched yet
            matchWords = this.dict.get(content[i].charAt(j) + "");
            /* No match found */
            if (matchWords.isEmpty())
              writer.print(content[i].charAt(j));
            else {
              /* Get the first match */
              enWord = (String) matchWords.toArray()[0];
              writer.print(String.format(TAG, enWord, content[i].charAt(j), enWord));
            }
            j++;
          } else {
            /* Obtain the Chinese word */
            cnWord = content[i].substring(j, nextPos);
            matchWords = this.dict.get(cnWord);
            /* Special case for law */
            if(nextPos + LAW_LNG < content[i].length() &&
                (content[i].substring(nextPos, nextPos +
                    LAW_LNG).equals(LAW_CHN) || content[i].substring(nextPos,
                        nextPos + LAW_LNG).equals(LAW_CHN_Q))) {
              cnWord = content[i].substring(j, nextPos + 1);
              enWord = (String) matchWords.toArray()[0] + LAW_ENG;
              nextPos++;
            }
            else
              enWord = (String) matchWords.toArray()[0];
            if(!enWord.contains(IGNORE))
              writer.print(String.format(TAG, enWord, cnWord, enWord));
            else
              writer.print(cnWord);
            j = nextPos; // Skip to the start of the next word
          }
        }
        if(i < content.length - 1) writer.print("¡£");
      }
      writer.print("}");
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Driver of the document tagger that adds tags for all files under input
   * using given dictionary and output results to the output directory.
   * 
   * @param args - command line arguments
   *        arg1 - dictionary file
   *        arg2 - input directory
   *        arg3 - output directory
   */
  public static void main(String args[]) {
    if(args.length != NUM_ARGS) throw new IllegalArgumentException();
    
    HashMultimap<String, String> map = HashMultimap.create();
    docTagger tagger = new docTagger(map, null, 0);
    long start = System.nanoTime();
    tagger.loadDict(args[0]);
    long end = System.nanoTime();
    System.out.println("Dictionary loaded in " + (end - start) + " ns.");
    //tagger.getStats();
    File folder = new File(args[1]);
    String[] fileList = folder.list();
    for(int i = 0; i < fileList.length; i++) {
      tagger.process(args[1] + "\\" + fileList[i], args[OUTPUT_IDX] + "\\" +
        fileList[i] + OUT_PREFIX, 12);
    }
    end = System.nanoTime();
    System.out.println("Total time: " + (end - start) + " ns.");
  }
}