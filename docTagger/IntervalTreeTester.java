/**
 * Simple unit tests for the IntervalSearcTree
 * @author Haoran Sun
 * @since 01/26/2017
 */
package docTagger;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class IntervalTreeTester {
  private IntervalTree<Interval> it;

  /**
   * Set up testing fixture for
   * 
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    this.it = new IntervalTree<>();
    this.it.insert(new IndexInterval(0, 4));
    this.it.insert(new IndexInterval(5, 7));
    this.it.insert(new IndexInterval(10, 12));
    this.it.insert(new IndexInterval(18, 20));
    this.it.insert(new IndexInterval(15, 17));
  }

  /**
   * Test query overlapping interval
   */
  @Test
  public void testQuery() {
    assertTrue(it.contains(new IndexInterval(10, 12)));
    assertFalse(it.contains(new IndexInterval(0, 12)));

    assertEquals(8, it.nextAvailable(new IndexInterval(5, 6)));
    assertEquals(13, it.nextAvailable(new IndexInterval(8, 12)));
    assertEquals(8, it.nextAvailable(new IndexInterval(4, 6)));
  }

}
