package origami.nez.peg;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import origami.nez.parser.Parser;
import origami.nez.parser.ParserFactory;
import origami.trait.OStringOut;

public class OGrammar extends AbstractList<OProduction> implements OStringOut {

	protected String id;
	protected OGrammar parent;
	protected ArrayList<String> nameList;
	protected HashMap<String, Expression> exprMap;

	public OGrammar(String name, OGrammar parent) {
		this.parent = parent;
		this.nameList = new ArrayList<>(16);
		this.exprMap = new HashMap<>();
		this.id = name == null ? "g" + Objects.hashCode(this) : name;
	}

	public OGrammar(String name) {
		this(name, null);
	}

	public OGrammar() {
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

	public OProduction getProduction(String name) {
		Expression e = this.exprMap.get(name);
		if (e != null) {
			return new OProduction(this, name, e);
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
	public final OProduction get(int index) {
		String name = this.nameList.get(index);
		return this.getProduction(name);
	}

	public final OProduction getStartProduction() {
		if (size() > 0) {
			return this.get(0);
		}
		return new OProduction(this, "EMPTY", Expression.defaultEmpty);
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

	public final OProduction[] getAllProductions() {
		ArrayList<OProduction> l = new ArrayList<>();
		for (OProduction p : this) {
			l.add(p);
		}
		if (this.parent != null) {
			HashMap<String, OProduction> map = new HashMap<>();
			for (OProduction p : l) {
				map.put(p.getLocalName(), p);
			}
			for (OGrammar g = this.parent; g != null; g = g.parent) {
				for (OProduction p : l) {
					if (!map.containsKey(p.getLocalName())) {
						l.add(p);
						map.put(p.getLocalName(), p);
					}
				}
			}
		}
		return l.toArray(new OProduction[l.size()]);
	}

	@Override
	public final String toString() {
		return OStringOut.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.id);
		for (String name : nameList) {
			sb.append(" ");
			sb.append(name);
			sb.append(" = ");
			OStringOut.append(sb, exprMap.get(name));
		}
	}

	public void replaceAll(List<OProduction> l) {
		this.nameList = new ArrayList<>(l.size());
		this.exprMap.clear();
		for (OProduction p : l) {
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

	/**
	 * NezParser
	 */

	public static final Parser NezParser;

	static {
		OGrammar grammar = new OGrammar("nez");
		ParserFactory factory = new ParserFactory();
		factory.setVerboseMode(false);
		new NezGrammar().load(factory, grammar, "Start");
		// grammar.dump();
		NezParser = grammar.newParser();
	}

	public void dump() {
		for (OProduction p : this) {
			System.out.println(p);
		}
	}

}
