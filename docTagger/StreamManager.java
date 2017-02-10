/**
 * Provide multi-threading task management for docTagger
 * @author Haoran Sun
 * @since 02/09/2017
 */

package docTagger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  final ExecutorService es = Executors.newFixedThreadPool(nThreads);
  
  private Scanner sc;
  private HashMultimap<String, String> dict;
  
  /**
   * Constructor
   */
  StreamManager() {
    this.dict = HashMultimap.create();
  }
  
  /**
   * Divide huge file streams into several small text streams and use multiple
   * threads to annotate the files without matching given lists of fields.
   * @param stream - main file stream
   * @param dictName - dictionary name
   * @param fields - exception fields
   * @throws InterruptedException
   */
  public void process(SequenceInputStream stream, String dictName,
      List<String> fields) throws InterruptedException {
    long start = System.nanoTime();
    this.loadDict(dictName);
    long end = System.nanoTime();
    this.sc = new Scanner(stream, StandardCharsets.UTF_8.toString());
    this.sc.useDelimiter("}"); //File delimiter
    System.out.println("Dictionary loaded in " + (end - start) + " ns.");
    this.manageProcess(es);
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
  private void manageProcess(Executor e) throws InterruptedException {
    ExecutorCompletionService<Integer> ecs = new
        ExecutorCompletionService<>(es);
    int id = 0; //Process id, used for outputting file
    
    /**
     * Assign tasks to each thread
     */
    for(int i = 0; i < nThreads; i++) {
      if(!this.sc.hasNext()) break;
      ecs.submit(new docTagger(this.dict, this.sc.next(), id++), DONE);
    }
    /**
     * Read all files and send next file to available thread
     */
    while(this.sc.hasNext()) {
      ecs.take();
      ecs.submit(new docTagger(this.dict, this.sc.next(), id++), DONE);
    }
    
    this.sc.close();
    es.shutdown();
  }
}