package blue.origami.nezcc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import blue.nez.peg.Expression;
import blue.nez.peg.Stateful;
import blue.nez.peg.Typestate;
import blue.nez.peg.expression.PNonTerminal;
import blue.origami.util.OCommonWriter;

abstract class CodeSection<C> {

	private boolean debug = System.getenv("DEBUG") != null;

	protected boolean isDebug() {
		return this.debug;
	}

	private OCommonWriter out = new OCommonWriter();

	protected void open(String file) throws IOException {
		this.out.open(file);
	}

	protected void write(Object value) {
		this.out.p(value);
	}

	protected void writeLine(String format, Object... args) {
		if (args.length == 0) {
			this.out.p(format);
			this.out.println();
		} else {
			this.out.println(format, args);
		}
	}

	protected void writeResource(String path, String... stringReplacements) throws IOException {
		this.out.importResourceContent(path, stringReplacements);
		this.writeSection(null);
	}

	protected void showResource(String path, String... stringReplacements) throws IOException {
		this.out.showResourceContent(path, stringReplacements);
	}

	protected SourceSection head = new SourceSection();

	protected SourceSection lib = new SourceSection();

	private SourceSection body = this.head;

	protected void writeSection(C code) {
		if (code == null) {
			this.body.L("");
		} else {
			this.body.L(code.toString());
		}
	}

	protected String Indent(String stmt) {
		return this.body.Indent(this.s("\t"), stmt);
	}

	protected void incIndent() {
		this.body.incIndent();
	}

	protected void decIndent() {
		this.body.decIndent();
	}

	protected HashMap<String, SourceSection> sectionMap = new HashMap<>();

	public boolean isDefinedSection(String funcName) {
		return this.sectionMap.containsKey(funcName);
	}

	protected final String RuntimeLibrary = null;

	protected SourceSection openSection(String funcName) {
		SourceSection prev = this.body;
		if (funcName == this.RuntimeLibrary) {
			this.body = this.lib;
		} else {
			this.body = new SourceSection();
			this.sectionMap.put(funcName, this.body);
		}
		return prev;
	}

	protected void closeSection(SourceSection prev) {
		this.body = prev;
	}

	private String currentFuncName = null;

	void setCurrentFuncName(String funcName) {
		this.currentFuncName = funcName;
		this.u = 0;
	}

	String getCurrentFuncName() {
		return this.currentFuncName;
	}

	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;
	final static int CNT = 1 << 3;
	final static int EMPTY = 1 << 4;

	protected int varStacks(int stacks, Expression e) {
		if (Typestate.compute(e) != Typestate.Unit) {
			stacks |= TREE;
		}
		if (Stateful.isStateful(e)) {
			stacks |= STATE;
		}
		return stacks;
	}

	protected String[] getStackNames(int stacks) {
		ArrayList<String> l = new ArrayList<>();
		if ((stacks & POS) == POS) {
			l.add(this.s("pos"));
		}
		if ((stacks & TREE) == TREE) {
			l.add(this.s("treeLog"));
			l.add(this.s("tree"));
		}
		if ((stacks & STATE) == STATE) {
			l.add(this.s("state"));
		}
		if ((stacks & CNT) == CNT) {
			l.add(this.s("cnt"));
		}
		return l.toArray(new String[l.size()]);
	}

	private int u = 0;

	public final int varSuffix() {
		return this.u++;
	}

	public abstract C V(String name);

	HashMap<String, String> symbolMap = new HashMap<>();

	protected boolean isDefined(String key) {
		return this.symbolMap.containsKey(key);
	}

	protected void defineSymbol(String key, String symbol) {
		if (!this.isDefined(key)) {
			this.symbolMap.put(key, symbol);
		}
	}

	protected void defineVariable(String name, String type) {
		String key = "T" + name;
		if (!this.isDefined(key)) {
			this.symbolMap.put(key, type);
		}
	}

	protected String T(String varname) {
		String key = "T" + varname;
		if (!this.isDefined(key)) {
			return null;
		}
		return this.s(key);
	}

	protected String getSymbol(String key) {
		return this.symbolMap.get(key);
	}

	protected String s(String key) {
		return this.symbolMap.getOrDefault(key, key);
	}

	protected void defineFunction(String funcName) {
		this.symbolMap.put(funcName, funcName);
	}

	protected String getConstName(String typeName, String typeLiteral) {
		if (typeName == null) {
			return typeLiteral;
		}
		String key = typeName + typeLiteral;
		String constName = this.symbolMap.get(key);
		if (constName == null) {
			constName = "c" + this.symbolMap.size();
			this.symbolMap.put(key, constName);
			SourceSection body = this.body;
			this.body = this.head;
			this.declConst(typeName, constName, typeLiteral);
			this.body = body;
		}
		return constName;
	}

	protected abstract void declConst(String typeName, String constName, String literal);

	// function
	HashMap<String, String> exprFuncMap = new HashMap<>();

	protected String getFuncName(Expression e) {
		if (e instanceof PNonTerminal) {
			String uname = ((PNonTerminal) e).getUniqueName();
			if (uname.indexOf('"') > 0) {
				String funcName = this.symbolMap.get(uname);
				if (funcName == null) {
					funcName = "t" + this.symbolMap.size();
					this.symbolMap.put(uname, funcName);
				}
				return funcName;
			}
			return uname.replace(':', '_').replace('.', '_').replace('&', '_');
		}
		String key = e.toString();
		String name = this.exprFuncMap.get(key);
		if (name == null) {
			name = "e" + this.exprFuncMap.size();
			this.exprFuncMap.put(key, name);
		}
		return name;
	}

	HashSet<String> crossRefNames = new HashSet<>();
	HashMap<String, HashSet<String>> depsMap = new HashMap<>();
	// HashMap<String, Integer> memoPointMap = new HashMap<>();

	protected final void addFunctionDependency(String sour, String dest) {
		if (sour != null) {
			HashSet<String> set = this.depsMap.get(sour);
			if (set == null) {
				set = new HashSet<>();
				this.depsMap.put(sour, set);
			}
			set.add(dest);
		}
	}

	ArrayList<String> sortFuncList(String start) {
		class TopologicalSorter {
			private final HashMap<String, HashSet<String>> nodes;
			private final LinkedList<String> result;
			private final HashMap<String, Short> visited;
			private final Short Visiting = 1;
			private final Short Visited = 2;

			TopologicalSorter(HashMap<String, HashSet<String>> nodes) {
				this.nodes = nodes;
				this.result = new LinkedList<>();
				this.visited = new HashMap<>();
				for (Map.Entry<String, HashSet<String>> e : this.nodes.entrySet()) {
					if (this.visited.get(e.getKey()) == null) {
						this.visit(e.getKey(), e.getValue());
					}
				}
			}

			private void visit(String key, HashSet<String> nextNodes) {
				this.visited.put(key, this.Visiting);
				if (nextNodes != null) {
					for (String nextNode : nextNodes) {
						Short v = this.visited.get(nextNode);
						if (v == null) {
							this.visit(nextNode, this.nodes.get(nextNode));
						} else if (v == this.Visiting) {
							if (!key.equals(nextNode)) {
								// System.out.println("Cyclic " + key + " => " +
								// nextNode);
								CodeSection.this.crossRefNames.add(nextNode);
							}
						}
					}
				}
				this.visited.put(key, this.Visited);
				this.result.add(key);
			}

			public ArrayList<String> getResult() {
				return new ArrayList<>(this.result);
			}
		}
		TopologicalSorter sorter = new TopologicalSorter(this.depsMap);
		ArrayList<String> funcList = sorter.getResult();
		if (!funcList.contains(start)) {
			funcList.add(start);
		}
		this.depsMap.clear();
		return funcList;
	}

}