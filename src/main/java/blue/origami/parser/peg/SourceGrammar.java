package blue.origami.parser.peg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.nez.ast.Source;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.parser.ParserSource;

public class SourceGrammar extends Grammar {

	public SourceGrammar(String name, SourceGrammar parent) {
		super(name, parent);
	}

	public SourceGrammar(String name) {
		super(name, null);
	}

	public SourceGrammar() {
		super(null, null);
	}

	/* grammar management */

	private HashMap<String, SourceGrammar> grammarMap = null;

	@Override
	public Grammar[] getLocalGrammars() {
		if (this.grammarMap == null) {
			return super.getLocalGrammars();
		}
		Grammar[] gs = new Grammar[this.grammarMap.size()];
		int c = 0;
		for (String name : this.grammarMap.keySet()) {
			gs[c] = this.grammarMap.get(name);
			c++;
		}
		return gs;
	}

	@Override
	protected SourceGrammar getLocalGrammar(String name) {
		if (this.grammarMap != null) {
			return this.grammarMap.get(name);
		}
		return null;
	}

	@Override
	public SourceGrammar newLocalGrammar(String name) {
		SourceGrammar g = new SourceGrammar(name, this);
		if (this.grammarMap == null) {
			this.grammarMap = new HashMap<>();
		}
		this.grammarMap.put(name, g);
		return g;
	}

	private ArrayList<String> publicList = null;

	@Override
	public void addPublicProduction(String name) {
		if (this.publicList == null) {
			this.publicList = new ArrayList<>(8);
		}
		this.publicList.add(name);
	}

	@Override
	public Production[] getPublicProductions() {
		Production[] ps = new Production[this.publicList == null ? 0 : this.publicList.size()];
		if (this.publicList != null) {
			for (int i = 0; i < this.publicList.size(); i++) {
				ps[i] = this.getProduction(this.publicList.get(i));
			}
		}
		return ps;
	}

	// ----------------------------------------------------------------------

	public final static Grammar loadFile(String file, String[] paths) throws IOException {
		SourceGrammar g = new SourceGrammar(SourcePosition.extractFileName(file));
		GrammarParser parser = new GrammarParser();
		parser.importSource(g, ParserSource.newFileSource(file, paths));
		return g;
	}

	public final static Grammar loadSource(Source s) throws IOException {
		SourceGrammar g = new SourceGrammar(SourcePosition.extractFileName(s.getResourceName()));
		GrammarParser parser = new GrammarParser();
		parser.importSource(g, s);
		return g;
	}

}
