package origami.nez.peg;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import origami.nez.ast.Source;
import origami.nez.parser.CommonSource;
import origami.nez.parser.Parser;
import origami.nez.parser.ParserFactory;
import origami.trait.StringCombinator;

public class Grammar extends AbstractList<Production> implements StringCombinator {

	protected String id;
	protected Grammar parent;
	protected ArrayList<String> nameList;
	protected HashMap<String, Expression> exprMap;

	public Grammar(String name, Grammar parent) {
		this.parent = parent;
		this.nameList = new ArrayList<>(16);
		this.exprMap = new HashMap<>();
		this.id = name == null ? "g" + Objects.hashCode(this) : name;
	}

	public Grammar(String name) {
		this(name, null);
	}

	public Grammar() {
		this(null, null);
	}

	public String getName() {
		return id;
	}

	public String getUniqueName(String name) {
		return id + ":" + name;
	}

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
		if (size() > 0) {
			return this.get(0);
		}
		return new Production(this, "EMPTY", Expression.defaultEmpty);
	}

	public final boolean hasProduction(String name) {
		return this.getExpression(name) != null;
	}

	public final void addProduction(String name, Expression e) {
		if (!exprMap.containsKey(name)) {
			this.nameList.add(name);
		}
		exprMap.put(name, e);
	}

	public final void setExpression(String name, Expression e) {
		if (!exprMap.containsKey(name)) {
			this.nameList.add(name);
		}
		exprMap.put(name, e);
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

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.id);
		for (String name : nameList) {
			sb.append(" ");
			sb.append(name);
			sb.append(" = ");
			StringCombinator.append(sb, exprMap.get(name));
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
		return loadFile(file, null);
	}

	public final static Grammar loadFile(String file, String[] paths) throws IOException {
		Grammar g = new Grammar(file);
		GrammarParser parser = new GrammarParser(g);
		parser.importSource(CommonSource.newFileSource(file, paths));
		return g;
	}

	public final static Grammar loadSource(Source s) throws IOException {
		Grammar g = new Grammar(s.getResourceName());
		GrammarParser parser = new GrammarParser(g);
		parser.importSource(s);
		return g;
	}

	/**
	 * Create a new parser
	 * 
	 * @param options
	 * @return
	 */

	public final Parser newParser() {
		return new Parser(new ParserFactory(), this.getStartProduction());
	}

	public final Parser newParser(String name) {
		return new Parser(new ParserFactory(), this.getProduction(name));
	}
	

	public void dump() {
		for (Production p : this) {
			System.out.println(p);
		}
	}


}
