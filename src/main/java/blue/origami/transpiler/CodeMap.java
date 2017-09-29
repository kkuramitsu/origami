package blue.origami.transpiler;

import java.util.List;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarLogger;
import blue.origami.util.ODebug;

public class CodeMap implements NameInfo {
	public final static CodeMap Null = null;
	public static final CodeMap StupidArrow = new CodeMap(CastCode.STUPID, "%s", "stupid", Ty.tVoid, Ty.tVoid);

	protected int acc;
	public final static int Mask = 0xffff;
	public final static int Impure = Mask + 1;
	public final static int Effect = Impure << 1;
	public final static int Faulty = Impure << 2;
	public final static int Error = Impure << 3;
	public final static int Used = Impure << 4;
	private final static int ParamChecked = Impure << 5;
	private final static int Generic = Impure << 6;
	private final static int Mutation = Impure << 7;

	// parameter
	protected final String name;
	protected Ty[] paramTypes;
	protected Ty returnType;

	// template
	protected final String template;

	public CodeMap(int acc, String name, String template, Ty returnType, Ty... paramTypes) {
		this.acc = acc;
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		this.template = template;
		assert (this.returnType != null) : this;
		assert (!this.is(ParamChecked));
	}

	public CodeMap(String name, String template, Ty returnType, Ty... paramTypes) {
		this(0, name, template, returnType, paramTypes);
	}

	public CodeMap(String template) {
		this(0, template, template, Ty.tVoid, TArrays.emptyTypes);
	}

	public boolean is(int flag) {
		return (this.acc & flag) == flag;
	}

	public void set(int flag, boolean b) {
		if (b) {
			this.set(flag);
		} else {
			this.unset(flag);
		}
	}

	public void set(int flag) {
		this.acc |= flag;
	}

	public void unset(int flag) {
		this.acc = this.acc & ~flag;
	}

	public String getName() {
		return this.name;
	}

	public Ty getReturnType() {
		return this.returnType;
	}

	public void setReturnType(Ty ret) {
		this.unset(ParamChecked);
		this.returnType = ret;
	}

	public boolean isAbstract() {
		return this.template.length() == 0;
	}

	public boolean isGeneric() {
		this.checkParamTypes();
		return this.is(Generic);
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public void setParamTypes(Ty[] pats) {
		this.unset(ParamChecked);
		this.paramTypes = pats;
	}

	void checkParamTypes() {
		if (!this.is(ParamChecked)) {
			this.set(Generic, TArrays.testSomeTrue(t -> t.hasVar(), this.paramTypes));
			this.set(Mutation, this.paramTypes.length > 0 && this.paramTypes[0].isMutable());
			this.set(ParamChecked);
		}
		assert (this.is(ParamChecked));
	}

	@Override
	public void used(TEnv env) {
		this.set(Used);
	}

	public final boolean isUnused() {
		return !this.is(Used);
	}

	public boolean isMutation() {
		this.checkParamTypes();
		return this.is(Mutation);
	}

	public FuncTy getFuncType() {
		return Ty.tFunc(this.returnType, this.paramTypes);
	}

	public int mapCost() {
		return this.acc & Mask;
	}

	public CodeMap setMapCost(int cost) {
		this.acc |= (cost & Mask);
		return this;
	}

	public String getDefined() {
		return this.template;
	}

	public boolean isExpired() {
		return false;
	}

	public CodeMap generate(TEnv env, Ty[] params) {
		this.used(env);
		return this;
	}

	@Override
	public boolean isNameInfo(TEnv env) {
		return true;
	}

	@Override
	public Code newCode(TEnv env, Tree<?> s) {
		return new FuncRefCode(this.name, this).setSource(s);
	}

	// public abstract String format(Object... args);

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		sb.append("::");
		FuncTy.stringfy(sb, this.getReturnType(), this.getParamTypes());
		// if (!this.isPure()) {
		// sb.append("@");
		// }
		sb.append(" = ");
		sb.append(this.getDefined());
		return sb.toString();
	}

	public static CodeMap select(TEnv env, List<CodeMap> founds, Ty ret, Ty[] p, int maxCost) {
		CodeMap selected = null;
		boolean allowAbstractMatch = TArrays.testSomeTrue(t -> t.hasVar(), p);
		// ODebug.trace("unselected=%s", TArrays.testSomeTrue(t -> t.hasVar(),
		// p));
		int mapCost = maxCost - 1;
		for (int i = 0; i < founds.size(); i++) {
			CodeMap next = founds.get(i);
			if (next.isAbstract() && !allowAbstractMatch) {
				continue;
			}
			int nextCost = match(env, next, ret, p, maxCost);
			ODebug.log(() -> ODebug.p("cost=%d,%s", nextCost, next));
			if (nextCost < mapCost) {
				mapCost = nextCost;
				selected = next;
			}
			if (mapCost == 0) {
				break;
			}
		}
		if (allowAbstractMatch) {
			CodeMap abst = founds.get(founds.size() - 1);
			if (abst != selected && abst.isAbstract()) {
				int nextCost = match(env, abst, ret, p, maxCost);
				if (nextCost <= mapCost) {
					ODebug.log(() -> ODebug.p("ABSTRACT cost=%s,%s", nextCost, abst));
					return abst;
				}
			}
		}
		return (mapCost >= maxCost) ? null : selected;
	}

	static int match(TEnv env, CodeMap tp, Ty ret, Ty[] params, int maxCost) {
		int mapCost = 0;
		VarDomain dom = null;
		VarLogger logs = new VarLogger();
		Ty[] p = tp.getParamTypes();
		Ty codeRet = tp.getReturnType();
		if (tp.isGeneric()) {
			dom = new VarDomain(p);
			p = dom.dupParamTypes(p, null);
			codeRet = dom.dupRetType(codeRet);
		}
		for (int i = 0; i < params.length; i++) {
			mapCost += env.mapCost(env, params[i], p[i], logs);
			// ODebug.trace("mapCost[%d]=%d %s => %s", i, mapCost, params[i], p[i]);
			if (mapCost >= maxCost) {
				logs.abort();
				return mapCost;
			}
		}
		if (ret.isSpecific()) {
			mapCost += env.mapCost(env, codeRet, ret, logs);
			// ODebug.trace("mapCost[ret]=%d %s => %s", mapCost, codeRet, ret);
		}
		// if (dom != null) {
		// mapCost += dom.mapCost();
		// }
		logs.abort();
		// ODebug.trace("mapCost=%d %s => %s", mapCost, codeRet, ret);
		return mapCost;
	}

}
