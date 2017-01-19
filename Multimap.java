import java.util.Hashtable;
import java.util.LinkedList;

/**
 * A class that implement Multimap using Hashtable
 * @author Haoran Sun
 * @since 01/14/2017
 */
public class Multimap {
	static final int SIZE = 2003;
	
	private Hashtable<String, LinkedList<String>> table;
	
	/**
	 * Default constructor
	 * Initilize with default size
	 */
	public Multimap() {
		this(SIZE);
	}
	
	/**
	 * 1-arg constructor that specified initial capacity
	 * @param capacity initial capacity
	 */
	public Multimap(int capacity) {
		this.table = new Hashtable<String, LinkedList<String>>(capacity);
	}
	
	/**
	 * Insert a word to the map according to the specified key
	 * @param key a Chinese word
	 * @param word the corresponding English word
	 */
	public void put(String key, String word) {
		if(key == null || word == null) throw new NullPointerException();
		
		LinkedList<String> tuple = this.table.get(key);
		if(tuple == null)
			this.table.put(key, tuple = new LinkedList<String>());
		tuple.add(word);
	}
	
	/**
	 * Retrieve the corresponding tuple of the given key
	 * @param key
	 * @return a tuple of English words
	 */
	public LinkedList<String> get(String key) {
		return this.table.get(key);
	}
}