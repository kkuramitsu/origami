package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TLetCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TParamCode;
import blue.origami.util.OConsole;
import blue.origami.util.OLog;

public class TGenerator {
	protected SourceSection head = new SourceSection();
	protected SourceSection lib = new SourceSection();
	// private SourceSection body = this.head;

	public void defineConst(Transpiler env, boolean isPublic, String name, TType type, TCode expr) {
		this.head.pushLine(env.format("const", "%1$s %2$s = %3$s", type.strOut(env), name, expr.strOut(env)));
	}

	private String currentFuncName = null;

	protected HashMap<String, SourceSection> sectionMap = new HashMap<>();

	public boolean isDefinedSection(String funcName) {
		return this.sectionMap.containsKey(funcName);
	}

	String getCurrentFuncName() {
		return this.currentFuncName;
	}

	public void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, TType[] paramTypes,
			TType returnType, TCode code) {
		String params = "";
		if (paramTypes.length > 0) {
			String delim = env.getSymbolOrElse(",", ",");
			StringBuilder sb = new StringBuilder();
			sb.append(env.format("param", "%1$s %2$s", paramTypes[0].strOut(env), paramNames[0] + 0));
			for (int i = 1; i < paramTypes.length; i++) {
				sb.append(delim);
				sb.append(env.format("param", "%1$s %2$s", paramTypes[i].strOut(env), paramNames[i] + i));
			}
			params = sb.toString();
		}
		SourceSection sec = new SourceSection();
		this.sectionMap.put(name, sec);
		this.currentFuncName = name;
		sec.pushLine(env.format("function", "%1$s %2$s(%3$s) {", returnType.strOut(env), name, params));
		sec.incIndent();
		sec.pushLine(env.format("return", "%s", code.strOut(env)));
		sec.decIndent();
		sec.pushLine(env.getSymbol("end function", "end", "}"));
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
								TGenerator.this.crossRefNames.add(nextNode);
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

	public void generateExpression(TEnv env, Tree<?> t) {
		TCode code = env.typeTree(env, t);
		SourceSection topLevel = new SourceSection();
		code.emitCode(env, topLevel);
		if (code.getType() != TType.tUnit) {
			OConsole.println("(%s) %s", code.getType(), OConsole.bold(topLevel.toString()));
		}
	}

	protected void log(String line, Object... args) {
		OConsole.println(line, args);
	}

}

class SourceSection implements TCodeSection {

	StringBuilder sb = new StringBuilder();
	int indent = 0;

	void incIndent() {
		this.indent++;
	}

	void decIndent() {
		assert (this.indent > 0);
		this.indent--;
	}

	String Indent(String tab, String stmt) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.indent; i++) {
			sb.append(tab);
		}
		sb.append(stmt);
		return sb.toString();
	}

	public void pushLine(String line) {
		this.sb.append(this.Indent("  ", line + "\n"));
	}

	@Override
	public void push(TCode t) {
		// TODO Auto-generated method stub
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

	@Override
	public void pushLog(OLog log) {
		System.out.println(log);
	}

	// Asm compatible

	private TEnv env;

	@Override
	public void push(String t) {
		this.sb.append(t);
	}

	@Override
	public void pushBool(TBoolCode code) {
		this.push(code.strOut(this.env));
	}

	@Override
	public void pushInt(TIntCode code) {
		this.push(code.strOut(this.env));
	}

	@Override
	public void pushDouble(TDoubleCode code) {
		this.push(code.strOut(this.env));
	}

	@Override
	public void pushCast(TCastCode code) {
		this.push(code.strOut(this.env));
	}

	@Override
	public void pushName(TNameCode code) {
		this.push(code.strOut(this.env));
	}

	@Override
	public void pushLet(TLetCode code) {
		this.push(code.strOut(this.env));
	}

	@Override
	public void pushCall(TParamCode code) {
		this.push(code.strOut(this.env));
	}

}
