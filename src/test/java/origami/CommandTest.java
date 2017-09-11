package origami;

import blue.origami.main.OCommand;

public class CommandTest {

	public void testExample() throws Throwable {
		OCommand.start("example", "-g", "konoha5.opeg");
	}

	public void testTest() throws Throwable {
		OCommand.start("test", "-g", "konoha5.opeg");
	}

	public void testMatch() throws Throwable {
		OCommand.start("match", "-g", "xml.opeg", "pom.xml");
	}

	public void testParse() throws Throwable {
		OCommand.start("parse", "-g", "xml.opeg", "pom.xml");
	}

}
