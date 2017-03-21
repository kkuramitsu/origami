package origami;

import origami.main.OCommand;

public class CommandTest {

	public void testExample() throws Exception {
		OCommand.start("example", "-g", "math.nez");
	}

	public void testTest() throws Exception {
		OCommand.start("test", "-g", "math.nez", "-D", "hoge=hoge");
	}

	public void testMatch() throws Exception {
		OCommand.start("match", "-g", "xml.nez", "pom.xml");
	}

	public void testParse() throws Exception {
		OCommand.start("parse", "-g", "xml.nez", "pom.xml");
	}

}
