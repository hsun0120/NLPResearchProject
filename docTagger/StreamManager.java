/**
 * Provide multi-threading task management for docTagger
 * @author Haoran Sun
 * @since 02/09/2017
 */

package docTagger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.*;
import com.opencsv.*;

/**
 * A class that manages word matching tasks by dividing huge file stream into
 * smaller portion.
 * This program uses fixed number of sub-threads, and next portion of the file
 * stream will be processed once a thread is free.
 */
public class StreamManager {
  static final int CHN_IDX = 2; //Index of Chinese word
  static final int nThreads = 4; //Number of threads used
  static final Integer DONE = new Integer(1); //Result indicator
  static final String DELI = "\\Z"; //File delimiter
  static final String END = "END";
  static final int CAPACITY = 1000;
  static final int SIZE = 40;
  
  private HashMultimap<String, String> dict;
  
  /**
   * Constructor
   */
  StreamManager() {
    this.dict = HashMultimap.create();
  }
  
  /**
   * Divide huge file streams into several small text streams and use multiple
   * threads to annotate the files.
   * @param stream - main file stream
   * @param dictName - dictionary name
   * @param fields - targeted fields
   * @param id - case id
   * @throws InterruptedException
   */
  public void process(Vector<String> stream, String dictName,
      List<String> fields, String id) throws InterruptedException {
    long start = System.nanoTime();
    this.loadDict(dictName);
    long end = System.nanoTime();
    System.out.println("Dictionary loaded in " + (end - start) + " ns.");
    final ExecutorService es = Executors.newFixedThreadPool(nThreads);
    this.manageProcess(es, stream, fields, id);
  }
  
  /**
   * Load the whole dictionary to a hash multi-map
   * 
   * @param dictName - the dictionary file name
   */
  private void loadDict(String dictName) {
    InputStreamReader in;
    CSVReader reader;
    try { /* Read and hash to the multi-map */
      in = new InputStreamReader(new FileInputStream(dictName),
          StandardCharsets.UTF_8);
      reader = new CSVReader(in);
      String[] dictLine;
      /* Use Chinese words as keys and English words as values */
      while ((dictLine = reader.readNext()) != null) {
        if (dictLine[1].contains("~"))
          dictLine[0] = dictLine[1].replace("~", dictLine[0]);
        if(dictLine[0].contains(" "))
          dictLine[0] = dictLine[0].replace(" ", "-");
        this.dict.put(dictLine[CHN_IDX], dictLine[0]);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Manage thread usage and assign tasks to available thread.
   * @param e - task executor
   * @throws InterruptedException
   */
  private void manageProcess(ExecutorService e, Vector<String> stream,
      List<String> fields, String id) throws InterruptedException {
    LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>(CAPACITY);
    
    int index = 0;
    while(index < stream.size() && index < SIZE) {
      Scanner sc;
      try {
        sc = new Scanner(new FileInputStream(stream.get(index)),
            StandardCharsets.UTF_8.toString());
        sc.useDelimiter(DELI);
        lbq.put(sc.next());
        sc.close();
      } catch (FileNotFoundException ex) {
        ex.printStackTrace();
      }
      index++;
    }
    
    if(index == stream.size())
      lbq.put(END);
    
    /* Assign tasks to each thread */
    for(int i = 0; i < nThreads; i++) {
      e.execute(new docTagger(this.dict, lbq, fields, id));
    }
    
    /* Read all files and send next file to available thread */
    while(index < stream.size()) {
      Scanner sc;
      try {
        sc = new Scanner(new FileInputStream(stream.get(index)),
            StandardCharsets.UTF_8.toString());
        sc.useDelimiter(DELI);
        lbq.put(sc.next());
        sc.close();
      } catch (FileNotFoundException ex) {
        ex.printStackTrace();
      }
      index++;
    }
    lbq.put(END);
    
    e.shutdown();
  }
}