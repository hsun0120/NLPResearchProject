/**
 * Driver and simulator for streaming environment
 * 
 * @author Haoran Sun
 * @since 02/10/2017
 */
package docTagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Simulate streaming environment and print out statistics.
 */
public class Simulator {
  static final int NUM_ARGS = 2;
  static final int CAP = 4021; //Capacity for the stream
  
  /**
   * main method to perform the simulation.
   * @param args - command line arguments
   *        args1 - dictionary name
   *        args2 - input directory
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException {
    if(args.length != NUM_ARGS) throw new IllegalArgumentException();
    
    long start = System.nanoTime();
    File folder = new File(args[1]);
    String[] fileList = folder.list(); //Get all the files of the source folder
    Vector<FileInputStream> fileStream = new Vector<>(CAP);
    
    /**
     * Open each file and add to file stream vector
     */
    for(int i = 0; i < fileList.length; i++) {
      try {
        fileStream.addElement(new FileInputStream(args[1] + "/" +
          fileList[i]));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    
    long end = System.nanoTime();
    System.out.println("File stream prepared in " + (end - start) + " ns.");
    
    /* Merge all streams together */
    SequenceInputStream sis = new SequenceInputStream(fileStream.elements());
    StreamManager mg = new StreamManager();
    ArrayList<String> fields = new ArrayList<>();
    fields.add("parties");
    fields.add("holding");
    fields.add("facts");
    fields.add("decision");
    mg.process(sis, args[0], fields); //Start annotating files
    end = System.nanoTime();
    System.out.println("Total time: " + (end - start) + " ns.");
  }
}