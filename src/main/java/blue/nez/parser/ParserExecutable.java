package blue.nez.parser;

import blue.nez.ast.Source;
import blue.nez.peg.Grammar;

public interface ParserExecutable {
	public Grammar getGrammar();

	public <T> T exec(ParserContext<T> ctx);

	public <T> ParserContext<T> newContext(Source s, long pos, TreeConstructor<T> newTree, TreeConnector<T> linkTree);

}