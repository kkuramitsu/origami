package origami;

import java.io.IOException;

import org.junit.Test;

import blue.nez.ast.Tree;
import blue.nez.parser.Parser;
import blue.nez.peg.Grammar;

public class ParserSample {

	@Test
	public void main() throws IOException {
		Grammar g = Grammar.loadFile("/blue/origami/grammar/math.opeg");
		Parser p = g.newParser();
		Tree<?> t = p.parse("1+2+3");
		System.out.println(t);
	}
}
