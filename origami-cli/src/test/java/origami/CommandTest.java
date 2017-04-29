package origami;

import org.junit.Ignore;
import org.junit.Test;

import blue.origami.main.OCommand;

public class CommandTest {

	@Test
	public void testExample() throws Throwable {
		OCommand.start("example", "-g", "math.opeg");
	}

	@Test
	public void testTest() throws Throwable {
		OCommand.start("test", "-g", "math.opeg", "-D", "hoge=hoge");
	}

	@Test
	@Ignore
	public void testMatch() throws Throwable {
		OCommand.start("match", "-g", "xml.opeg", "pom.xml");
	}

	@Test
	@Ignore
	public void testParse() throws Throwable {
		OCommand.start("parse", "-g", "xml.opeg", "pom.xml");
	}

}
