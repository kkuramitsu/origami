package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;

public class SourceGenerator extends Generator {
	protected SourceType ts;
	protected SourceSection head;
	protected SourceSection data;
	protected SourceSection eval;
	// private SourceSection body = this.head;

	public SourceGenerator(Transpiler tr) {
		this.ts = new SourceType(tr);
	}

	@Override
	public void init() {
		this.ts.initProperties();
	}

	@Override
	protected void setup() {
		this.head = new SourceSection(this.ts);
		this.data = new SourceSection(this.ts);
		this.eval = new SourceSection(this.ts);
		this.ts.setTypeDeclSection(this.data);
		this.secList = new ArrayList<>();
		this.secMap = new HashMap<>();
	}

	@Override
	protected Object wrapUp() {
		System.out.println(this.head.toString());
		System.out.println(this.data.toString());
		for (SourceSection sec : this.secList) {
			System.out.println(sec);
		}
		return this.eval.toString();
	}

	@Override
	public void emitTopLevel(TEnv env, Code code) {
		code = this.emitHeader(env, code);
		code.emitCode(env, this.eval);
	}

	@Override
	public CodeTemplate newConstTemplate(TEnv env, String lname, Ty ret) {
		String template = String.format(env.fmt("constname", "name", "%s"), lname);
		return new ConstTemplate(lname, ret, template);
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr) {
		this.data.pushIndent("");
		this.data.pushf(env, env.fmt("const", "%1$s %2$s = %3$s"), type, name, expr);
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
	public CodeTemplate newTemplate(TEnv env, String sname, String lname, Ty returnType, Ty... paramTypes) {
		String param = "";
		if (paramTypes.length > 0) {
			String delim = env.getSymbolOrElse(",", ",");
			StringBuilder sb = new StringBuilder();
			sb.append("%s");
			for (int i = 1; i < paramTypes.length; i++) {
				sb.append(delim);
				sb.append("%s");
			}
			param = sb.toString();
		}
		String template = env.format("funccall", "%s(%s)", lname, param);
		return new CodeTemplate(sname, returnType, paramTypes, template);
	}

	@Override
	public void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code) {
		SourceSection sec = new SourceSection(this.ts);
		this.secList.add(sec);
		this.secMap.put(name, sec);
		this.currentFuncName = name;
		Param p = new Param(0, paramNames, paramTypes);
		sec.pushIndent("");
		sec.pushf(env, env.fmt("function", "%1$s %2$s(%3$s) {"), returnType, name, p);
		sec.pushLine("");
		sec.pushBlock(env, code.addReturn());
		sec.pushIndentLine(env.getSymbol("end function", "end", "}"));
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
								SourceGenerator.this.crossRefNames.add(nextNode);
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
		// this.depsMap.clear();
		return funcList;
	}

}