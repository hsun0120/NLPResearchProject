package docTagger;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class DoublePipelinedBuffer {
  private static final int CAPACITY = 10000;
  private static final int LIMIT = 9500;
  
  private StringBuffer buffer1;
  private StringBuffer buffer2;
  private StringBuffer curr;
  private InputStream fileStream;
  private boolean isFinished;
  
  public DoublePipelinedBuffer() {
    this.buffer1 = new StringBuffer(CAPACITY);
    this.buffer2 = new StringBuffer(CAPACITY);
    curr = buffer1;
    this.isFinished = false;
  }
  
  public void read() {
    
  }
}