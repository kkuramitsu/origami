package blue.origami.transpiler.code;

import java.util.ArrayList;
import java.util.List;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.TVarDomain;
import blue.origami.transpiler.TVarType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.TCastCode.TBoxCode;
import blue.origami.transpiler.code.TCastCode.TConvTemplate;
import blue.origami.transpiler.code.TCastCode.TUnboxCode;
import blue.origami.util.ODebug;

public class TExprCode extends MultiTypedCode {

	private String name;

	public TExprCode(Template tp, TCode... args) {
		super(tp.getReturnType(), tp, args);
		this.name = tp.getName();
	}

	public TExprCode(String name, TCode... args) {
		super(TType.tUntyped, null, args);
		this.name = name;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushCall(env, this);
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		if (this.getType().isUntyped()) {
			final TCode[] params = this.args;
			List<Template> l = new ArrayList<>(8);
			env.findList(this.name, Template.class, l, (tt) -> tt.isGenerated() && tt.getParamSize() == params.length);
			// ODebug.trace("l = %s", l);
			if (l.size() == 0) {
				throw new TErrorCode("undefined %s%s", this.name, this.types(params));
			}
			if (l.size() == 1) {
				return this.asType(env, l.get(0).update(env, params), t);
			}
			boolean foundUntyped = false;
			for (int i = 0; i < params.length; i++) {
				TType pt = this.getCommonParamType(l, i);
				this.args[i] = this.args[i].asType(env, pt);
				if (this.args[i].isUntyped()) {
					foundUntyped = true;
				}
			}
			if (foundUntyped == true) {
				return this;
			}
			Template selected = l.get(0);
			int mapCost = this.checkMapCost(env, t, selected);
			ODebug.trace("cost=%d,%s", mapCost, selected);
			for (int i = 1; i < l.size(); i++) {
				if (mapCost > 0) {
					Template next = l.get(i);
					int nextCost = this.checkMapCost(env, t, next);
					ODebug.trace("nextcost=%d,%s", nextCost, next);
					if (nextCost < mapCost) {
						mapCost = nextCost;
						selected = next;
					}
				}
			}
			if (mapCost >= TCastCode.STUPID) {
				throw new TErrorCode("mismatched %s%s", this.name, this.types(params));
			}
			return this.asType(env, selected.update(env, params), t);
		}
		return super.asType(env, t);
	}

	private String types(TCode... params) {
		StringBuilder sb = new StringBuilder();
		for (TCode t : params) {
			sb.append(" ");
			sb.append(t.getType());
		}
		return sb.toString();
	}

	private TCode asType(TEnv env, Template tp, TType t) {
		if (tp != null) {
			TType[] p = tp.getParamTypes();
			TType ret = tp.getReturnType();
			if (tp.isGeneric()) {
				TVarDomain dom = new TVarDomain();
				TType[] gp = new TType[p.length];
				for (int i = 0; i < p.length; i++) {
					gp[i] = p[i].dup(dom);
				}
				ret = ret.dup(dom);
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, gp[i]);
					if (p[i] instanceof TVarType) {
						ODebug.trace("must upcast %s => %s", p[i], gp[i]);
						this.args[i] = new TBoxCode(gp[i], this.args[i]);
					}
				}
				this.setTemplate(tp);
				this.setType(ret);
				TCode result = this;
				if (tp.getReturnType() instanceof TVarType) {
					ODebug.trace("must downcast %s => %s", tp.getReturnType(), ret);
					result = new TUnboxCode(ret, result);
				}
				return result.asType(env, t);
			} else {
				for (int i = 0; i < this.args.length; i++) {
					this.args[i] = this.args[i].asType(env, p[i]);
				}
				this.setTemplate(tp);
				this.setType(ret);
				return super.asType(env, t);
			}
		}
		return this;
	}

	private TType getCommonParamType(List<Template> l, int n) {
		TType t = l.get(0).getParamTypes()[n];
		for (int i = 1; i < l.size(); i++) {
			if (!t.equals(l.get(i).getParamTypes()[n])) {
				return TType.tUntyped;
			}
		}
		return t;
	}

	private int checkMapCost(TEnv env, TType ret, Template tp) {
		int mapCost = 0;
		TVarDomain dom = null;
		TType[] p = tp.getParamTypes();
		if (tp.isGeneric()) {
			dom = new TVarDomain();
			TType[] gp = new TType[p.length];
			for (int i = 0; i < p.length; i++) {
				gp[i] = p[i].dup(dom);
			}
			p = gp;
		}
		for (int i = 0; i < this.args.length; i++) {
			TType f = this.args[i].getType();
			if (p[i].acceptType(f)) {
				continue;
			}
			TConvTemplate conv = env.findTypeMap(env, f, p[i]);
			// ODebug.trace("mapcost %s %s %d", f, p[i], conv.mapCost);
			mapCost += conv.mapCost;
			if (i < 2) {
				mapCost += conv.mapCost;
			}
			if (mapCost >= TCastCode.STUPID) {
				return mapCost;
			}
		}
		if (dom != null) {
			mapCost += dom.mapCost();
		}
		return mapCost;
	}
}