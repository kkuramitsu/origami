package blue.origami.parser.peg;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import blue.origami.main.MainOption;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.parser.Parser;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;
import blue.origami.util.OStrings;

public abstract class Grammar extends AbstractList<Production> implements OStrings {

	protected final String id;
	protected final Grammar parent;
	protected ArrayList<String> nameList;
	protected HashMap<String, Expression> exprMap;

	public Grammar(String name, Grammar parent) {
		this.parent = parent;
		this.nameList = new ArrayList<>(16);
		this.exprMap = new HashMap<>();
		String id = name == null ? "g" + Objects.hashCode(this) : SourcePosition.extractFileBaseName(name);
		if (parent != null) {
			id = parent.getName() + "." + id;
		}
		this.id = id;
	}

	// public Grammar(String name) {
	// this(name, null);
	// }
	//
	// public Grammar() {
	// this(null, null);
	// }

	public String getName() {
		return this.id;
	}

	public String getUniqueName(String name) {
		return this.id + ":" + name;
	}

	/* grammar management */

	public Grammar[] getLocalGrammars() {
		return new Grammar[0];
	}

	public final Grammar getGrammar(String name) {
		Grammar g = this.getLocalGrammar(name);
		if (g == null && this.parent != null) {
			return this.parent.getGrammar(name);
		}
		return g;
	}

	protected Grammar getLocalGrammar(String name) {
		return null;
	}

	public Grammar newLocalGrammar(String name) {
		ODebug.NotAvailable(this);
		return this;
	}

	/* expression */

	public Expression getExpression(String name) {
		Expression e = this.exprMap.get(name);
		if (e != null) {
			return e;
		}
		if (this.parent != null) {
			return this.parent.getExpression(name);
		}
		return null;
	}

	public Expression getLocalExpression(String name) {
		return this.exprMap.get(name);
	}

	public Production getProduction(String name) {
		Expression e = this.exprMap.get(name);
		if (e != null) {
			return new Production(this, name, e);
		}
		if (this.parent != null) {
			return this.parent.getProduction(name);
		}
		return null;
	}

	@Override
	public final int size() {
		return this.nameList.size();
	}

	@Override
	public final Production get(int index) {
		String name = this.nameList.get(index);
		return this.getProduction(name);
	}

	public final Production getStartProduction() {
		if (this.size() > 0) {
			return this.get(0);
		}
		return new Production(this, "EMPTY", Expression.defaultEmpty);
	}

	public final boolean hasProduction(String name) {
		return this.getExpression(name) != null;
	}

	public final void addProduction(String name, Expression e) {
		if (!this.exprMap.containsKey(name)) {
			this.nameList.add(name);
		}
		this.exprMap.put(name, e);
	}

	public final void setExpression(String name, Expression e) {
		if (!this.exprMap.containsKey(name)) {
			this.nameList.add(name);
		}
		this.exprMap.put(name, e);
	}

	public final Production[] getAllProductions() {
		ArrayList<Production> l = new ArrayList<>();
		for (Production p : this) {
			l.add(p);
		}
		if (this.parent != null) {
			HashMap<String, Production> map = new HashMap<>();
			for (Production p : l) {
				map.put(p.getLocalName(), p);
			}
			for (Grammar g = this.parent; g != null; g = g.parent) {
				for (Production p : l) {
					if (!map.containsKey(p.getLocalName())) {
						l.add(p);
						map.put(p.getLocalName(), p);
					}
				}
			}
		}
		return l.toArray(new Production[l.size()]);
	}

	/* public productions */

	public abstract void addPublicProduction(String name);

	public abstract Production[] getPublicProductions();

	@Override
	public final String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.id);
		for (String name : this.nameList) {
			sb.append(" ");
			sb.append(name);
			sb.append(" = ");
			OStrings.append(sb, this.exprMap.get(name));
		}
	}

	public void replaceAll(List<Production> l) {
		this.nameList = new ArrayList<>(l.size());
		this.exprMap.clear();
		for (Production p : l) {
			this.addProduction(p.getLocalName(), p.getExpression());
		}
	}

	// ----------------------------------------------------------------------

	private HashMap<String, MemoEntry> memoMap = new HashMap<>();

	private class MemoEntry {
		private MemoEntry prev;
		Object value;

		MemoEntry(MemoEntry prev, Object value) {
			this.prev = prev;
			this.value = value;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getProperty(String name, Class<T> c) {
		MemoEntry m = this.memoMap.get(name);
		for (; m != null; m = m.prev) {
			if (c.isInstance(m.value)) {
				return (T) m.value;
			}
		}
		return null;
	}

	public void setProperty(String name, Object value) {
		MemoEntry m = this.memoMap.get(name);
		this.memoMap.put(name, new MemoEntry(m, value));
	}

	// ----------------------------------------------------------------------

	public final static Grammar loadFile(String file) throws IOException {
		return SourceGrammar.loadFile(file, null);
	}

	// public final static Grammar loadFile(String file, String[] paths) throws
	// IOException {
	// Grammar g = new Grammar(file);
	// GrammarParser parser = new GrammarParser();
	// parser.importSource(g, ParserSource.newFileSource(file, paths));
	// return g;
	// }
	//
	// public final static Grammar loadSource(Source s) throws IOException {
	// Grammar g = new Grammar(s.getResourceName());
	// GrammarParser parser = new GrammarParser();
	// parser.importSource(g, s);
	// return g;
	// }

	/**
	 * Create a new parser
	 * 
	 * @param options
	 * @return
	 */

	public final Parser newParser() {
		return new Parser(this.getStartProduction(), new OOption());
	}

	public final Parser newParser(String start) {
		return this.newParser(start, new OOption());
	}

	public Parser newParser(OOption options) {
		String start = options.stringValue(MainOption.Start, null);
		if (start == null) {
			return new Parser(this.getStartProduction(), options);
		}
		return this.newParser(start, options);
	}

	public final Parser newParser(String name, OOption options) {
		Production p = this.getProduction(name);
		if (p != null) {
			return new Parser(p, options);
		}
		return null;
	}

	public void dump() {
		OConsole.println("dump grammar " + this.id + " size=" + this.nameList.size());
		for (Production p : this) {
			System.out.println(p);
		}
	}

}
