package blue.origami.transpiler.code;

import java.util.ArrayList;
import java.util.List;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.TCastCode.TConvTemplate;

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
			env.findList(this.name, Template.class, l, (tt) -> tt.isEnabled() && tt.getParamSize() == params.length);
			// ODebug.trace("l = %s", l);
			if (l.size() == 0) {
				return new TErrorCode("undefined %s%s", this.name, this.types(params));
			}
			if (l.size() == 1) {
				return this.asType(env, l.get(0).update(env, params), t);
			}
			for (int i = 0; i < params.length; i++) {
				TType pt = this.getCommonParamType(l, i);
				if (pt != null) {
					this.args[i] = this.args[i].asType(env, pt);
				}
			}
			Template selected = l.get(0);
			int mapCost = this.mapCost(env, t, selected);
			// System.out.println("cost=" + mapCost + ", " + selected);
			for (int i = 1; i < l.size(); i++) {
				if (mapCost > 0) {
					Template next = l.get(i);
					int nextCost = this.mapCost(env, t, next);
					// System.out.println("nextcost=" + nextCost + ", " + next);
					if (nextCost < mapCost) {
						selected = next;
						mapCost = nextCost;
					}
				}
			}
			if (mapCost >= TCastCode.STUPID) {
				// ODebug.trace("miss cost=%d %s", start.getMatchCost(), start);
				return new TErrorCode("mismatched %s%s", this.name, this.types(params));
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

	private TCode asType(TEnv env, Template tp, TType ret) {
		if (tp != null) {
			TType[] p = tp.getParamTypes();
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, p[i]);
			}
			this.setTemplate(tp);
			this.setType(tp.getReturnType());
			return super.asType(env, ret);
		}
		return this;
	}

	private TType getCommonParamType(List<Template> l, int n) {
		TType t = l.get(0).getParamTypes()[n];
		for (int i = 1; i < l.size(); i++) {
			if (!t.equals(l.get(i).getParamTypes()[n])) {
				return null;
			}
		}
		return t;
	}

	private int mapCost(TEnv env, TType ret, Template tp) {
		boolean isUntyped = false;
		int mapCost = 0;
		TType[] p = tp.getParamTypes();
		for (int i = 0; i < this.args.length; i++) {
			TType f = this.args[i].getType();
			if (f.isUntyped()) {
				isUntyped = true;
				continue;
			}
			if (p[i].accept(this.args[i])) {
				continue;
			}
			TConvTemplate conv = env.findTypeMap(env, f, p[i]);
			mapCost += conv.mapCost;
			if (i < 2) {
				mapCost += conv.mapCost;
			}
		}
		if (isUntyped) {
			if (!ret.isUntyped() && ret != TType.tVoid) {
				TConvTemplate conv = env.findTypeMap(env, tp.getReturnType(), ret);
				return mapCost + (conv.mapCost * 2);
			}
			return mapCost + TCastCode.STUPID;
		}
		return mapCost;
	}

}