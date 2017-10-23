package origami;

import java.io.IOException;

import blue.origami.common.Tree;
import blue.origami.parser.Parser;
import blue.origami.parser.peg.Grammar;

public class ParserSample {

	public static void main(String[] arg) throws IOException {
		Grammar g = Grammar.loadFile("math.opeg");
		Parser p = g.newParser();
		Tree<?> t = p.parse("1+2+3");
		System.out.println(t);
	}
}
