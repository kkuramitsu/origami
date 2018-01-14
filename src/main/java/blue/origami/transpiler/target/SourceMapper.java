package blue.origami.transpiler.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import blue.origami.common.OWriter;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeMapper;
import blue.origami.transpiler.ConstMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;

public class SourceMapper extends CodeMapper {
	protected final SourceTypeMapper ts;
	protected SourceSection head;
	protected SourceSection data;
	protected SourceSection eval;
	protected OWriter writer;

	public SourceMapper(Transpiler tr, SourceTypeMapper ts) {
		super(tr, new SyntaxMapper());
		this.syntax.importSyntaxFile(tr.getPath("syntax.codemap"));
		this.ts = ts;
		ts.setSyntaxMapper(this.syntax);
	}

	@Override
	public void init() {
		this.ts.initProperties();
	}

	public SourceSection newSourceSection() {
		return new SourceSection(this.syntax, this.ts);
	}

	@Override
	protected void setup() {
		this.head = this.newSourceSection();
		this.data = this.newSourceSection();
		this.eval = this.newSourceSection();
		this.ts.setTypeSection(this.data);
		this.secList = new ArrayList<>();
		this.secMap = new HashMap<>();
		this.writer = new OWriter();
	}

	@Override
	public void emitTopLevel(Env env, Code code) {
		code = this.emitHeader(env, code);
		code.emitCode(this.eval);
	}

	@Override
	protected Object wrapUp() {
		this.writer.println(this.head.toString());
		this.writer.println(this.data.toString());
		for (SourceSection sec : this.secList) {
			this.writer.println(sec.toString());
		}
		String evaled = this.eval.toString();
		this.writer.println(evaled);
		this.writer.close();
		return evaled;
	}

	// Code Map

	@Override
	public CodeMap newConstMap(Env env, String lname, Ty ret) {
		String template = String.format(this.syntax.fmt("constname", "name", "%s"), lname);
		return new ConstMap(lname, template, ret);
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr) {
		this.data.pushIndent("");
		this.data.pushf(this.syntax.fmt("const", "%1$s %2$s = %3$s"), type, name, expr);
		this.data.pushLine("");
	}

	private String currentFuncName = null;

	protected ArrayList<SourceSection> secList = new ArrayList<>();
	protected HashMap<String, SourceSection> secMap = new HashMap<>();

	public boolean isDefinedSection(String funcName) {
		return this.secMap.containsKey(funcName);
	}

	String getCurrentFuncName() {
		return this.currentFuncName;
	}

	@Override
	public CodeMap newCodeMap(Env env, String sname, String lname, Ty returnType, Ty... paramTypes) {
		String param = "";
		if (paramTypes.length > 0) {
			String delim = this.syntax.symbol(",", ",");
			StringBuilder sb = new StringBuilder();
			sb.append("%s");
			for (int i = 1; i < paramTypes.length; i++) {
				sb.append(delim);
				sb.append("%s");
			}
			param = sb.toString();
		}
		String template = this.syntax.format(this.syntax.fmt("funccall", "%s(%s)"), lname, param);
		return new CodeMap(0, template, sname, returnType, paramTypes);
	}

	@Override
	public void defineFunction(Env env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code) {
		SourceSection sec = this.newSourceSection();
		this.secList.add(sec);
		this.secMap.put(name, sec);
		this.currentFuncName = name;
		sec.pushFuncDecl(name, returnType, paramNames, paramTypes, code);
	}

	HashSet<String> crossRefNames = new HashSet<>();
	HashMap<String, HashSet<String>> depsMap = new HashMap<>();

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
								SourceMapper.this.crossRefNames.add(nextNode);
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
		return funcList;
	}

}