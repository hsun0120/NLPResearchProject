package docTagger;
public class IndexInterval implements Interval {
	private int start, end;
	
	public IndexInterval(int start, int end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public int start() {
		return this.start;
	}

	@Override
	public int end() {
		return this.end;
	}
	
}