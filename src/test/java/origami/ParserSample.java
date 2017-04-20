package origami;

import java.io.IOException;

import blue.nez.ast.Tree;
import blue.nez.parser.Parser;
import blue.nez.peg.Grammar;

public class ParserSample {

	public static void main(String[] arg) throws IOException {
		Grammar g = Grammar.loadFile("math.opeg");
		Parser p = g.newParser();
		Tree<?> t = p.parse("1+2+3");
		System.out.println(t);
	}
}
