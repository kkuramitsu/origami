package blue.origami.transpiler;

import java.util.List;

import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarLogger;
import blue.origami.util.ODebug;

public abstract class Template {
	public final static Template Null = null;

	protected short cost = 0;

	protected boolean isPure;
	protected boolean isFaulty;
	protected boolean isError;
	protected boolean isUsed;

	// Skeleton
	protected boolean isGeneric;
	protected final String name;
	protected Ty[] paramTypes;
	protected Ty returnType;
	// private final String template;

	public Template(String name, Ty returnType, Ty... paramTypes) {
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		this.isGeneric = TArrays.testSomeTrue(t -> t.hasVar(), paramTypes);
		assert (this.returnType != null) : this;
	}

	public String getName() {
		return this.name;
	}

	public Ty getReturnType() {
		return this.returnType;
	}

	public boolean isAbstract() {
		return true;
	}

	public boolean isGeneric() {
		return this.isGeneric;
	}

	public int getParamSize() {
		return this.paramTypes.length;
	}

	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public void used(TEnv env) {
		this.isUsed = true;
	}

	public final boolean isUnused() {
		return !this.isUsed;
	}

	public boolean isMutation() {
		if (this.paramTypes.length > 0) {
			return this.paramTypes[0].isMutable();
		}
		return false;
	}

	public FuncTy getFuncType() {
		return Ty.tFunc(this.returnType, this.paramTypes);
	}

	public final boolean isPure() {
		return this.isPure;
	}

	public Template asPure(boolean pure) {
		this.isPure = pure;
		return this;
	}

	public final boolean isFaulty() {
		return this.isFaulty;
	}

	public Template asFaulty(boolean faulty) {
		this.isFaulty = faulty;
		return this;
	}

	public final boolean isError() {
		return this.isError;
	}

	public Template asError(boolean error) {
		this.isError = error;
		return this;
	}

	public int mapCost() {
		return this.cost;
	}

	public Template setMapCost(int cost) {
		this.cost = (short) cost;
		return this;
	}

	public abstract String getDefined();

	public boolean isExpired() {
		return false;
	}

	public Template generate(TEnv env, Ty[] params) {
		this.used(env);
		return this;
	}

	public abstract String format(Object... args);

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		for (Ty t : this.getParamTypes()) {
			sb.append(":");
			sb.append(t);
		}
		sb.append(":");
		sb.append(this.getReturnType());
		// if (!this.isPure()) {
		// sb.append("@");
		// }
		return sb.toString();
	}

	public static Template select(TEnv env, List<Template> founds, Ty ret, Ty[] p, int maxCost) {
		Template selected = null;
		boolean allowAbstractMatch = TArrays.testSomeTrue(t -> t.hasVar(), p);
		// ODebug.trace("unselected=%s", TArrays.testSomeTrue(t -> t.hasVar(),
		// p));
		int mapCost = maxCost - 1;
		for (int i = 0; i < founds.size(); i++) {
			Template next = founds.get(i);
			if (next.isAbstract() && !allowAbstractMatch) {
				continue;
			}
			ODebug.trace("trying %s", next);
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
			Template abst = founds.get(founds.size() - 1);
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

	static int match(TEnv env, Template tp, Ty ret, Ty[] params, int maxCost) {
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
			// ODebug.trace("mapCost=%d %s => %s", mapCost, params[i], p[i]);
			if (mapCost >= maxCost) {
				logs.abort();
				return mapCost;
			}
		}
		if (ret.isSpecific()) {
			mapCost += env.mapCost(env, codeRet, ret, logs);
		}
		// ODebug.trace("mapCost=%d %s => %s", mapCost, codeRet, ret);
		// if (dom != null) {
		// mapCost += dom.mapCost();
		// }
		// ODebug.trace("mapCost=%d %s => %s", mapCost, codeRet, ret);
		logs.abort();
		return mapCost;
	}

}
