import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class IntervalTreeTester {
	private IntervalTree<Interval> it;

	@Before
	public void setUp() throws Exception {
		this.it = new IntervalTree<>();
		this.it.insert(new IndexInterval(0, 4));
		this.it.insert(new IndexInterval(5, 7));
		this.it.insert(new IndexInterval(10, 12));
		this.it.insert(new IndexInterval(18, 20));
		this.it.insert(new IndexInterval(15, 17));
	}

	@Test
	public void testQuery() {
		assertTrue(it.contains(new IndexInterval(10, 12)));
		assertFalse(it.contains(new IndexInterval(0, 12)));
		
		assertTrue(it.overlaps(new IndexInterval(4, 5)));
		assertEquals(8, it.nextAvailable(new IndexInterval(6, 6)));
		assertEquals(12, it.nextAvailable(new IndexInterval(12, 12)));
	}

}
