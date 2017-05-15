package origami;

import java.io.IOException;

import blue.origami.nez.ast.Tree;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.peg.Grammar;

public class ParserSample {

	public static void main(String[] arg) throws IOException {
		Grammar g = Grammar.loadFile("math.opeg");
		Parser p = g.newParser();
		Tree<?> t = p.parse("1+2+3");
		System.out.println(t);
	}
}
