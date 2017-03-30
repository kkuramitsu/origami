package origami;

import blue.origami.main.OCommand;

public class CommandTest {

	public void testExample() throws Throwable {
		OCommand.start("example", "-g", "math.opeg");
	}

	public void testTest() throws Throwable {
		OCommand.start("test", "-g", "math.opeg", "-D", "hoge=hoge");
	}

	public void testMatch() throws Throwable {
		OCommand.start("match", "-g", "xml.opeg", "pom.xml");
	}

	public void testParse() throws Throwable {
		OCommand.start("parse", "-g", "xml.opeg", "pom.xml");
	}

}
