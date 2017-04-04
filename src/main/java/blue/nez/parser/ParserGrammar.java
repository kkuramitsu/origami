package blue.nez.parser;

import blue.nez.peg.Grammar;
import blue.nez.peg.Production;

class ParserGrammar extends Grammar {
	public boolean isBinary = false;

	ParserGrammar(String name, boolean isBinary) {
		super(name, null);
		this.isBinary = isBinary;
	}

	public boolean isBinary() {
		return this.isBinary;
	}

	@Override
	public String getUniqueName(String name) {
		return name;
	}

	@Override
	public void addPublicProduction(String name) {
	}

	@Override
	public Production[] getPublicProductions() {
		return new Production[0];
	}
}