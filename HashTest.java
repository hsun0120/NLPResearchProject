import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import com.opencsv.*;

public class HashTest {
	static final int CHN_IDX = 2;
	static final String FMT = "UTF-8";

	public static void main(String[] args) {
		Multimap table = new Multimap();
		try {
			InputStreamReader in = new InputStreamReader(new FileInputStream(args[0]), FMT);
			CSVReader reader = new CSVReader(in);
			String[] line;
			while ((line = reader.readNext()) != null) {
				table.insert(line[CHN_IDX], line[0]);
			}
			reader.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(table.get("∑œ÷π"));
	}
}