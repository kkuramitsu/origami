package origami;

import origami.main.Main;

public class CommandTest {

	// public void testExample() throws Throwable {
	// Main.start("example", "-g", "chibi.opeg");
	// }

	public void testExample() throws Throwable {
		Main.testMain("example", "-g", "json.opeg");
	}

	// public void testMatch() throws Throwable {
	// Main.start("match", "-g", "xml.opeg", "pom.xml");
	// }

	public void testParse() throws Throwable {
		Main.testMain("parse", "-g", "xml.opeg", "pom.xml");
	}

}
