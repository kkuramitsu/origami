package blue.origami.transpiler.code;

import java.util.ArrayList;
import java.util.List;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.VarDomain;
import blue.origami.transpiler.VarTy;
import blue.origami.transpiler.code.CastCode.TBoxCode;
import blue.origami.transpiler.code.CastCode.TUnboxCode;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class ExprCode extends CodeN {

	private String name;

	public ExprCode(Template tp, Code... args) {
		super(tp.getReturnType(), args);
		this.setTemplate(tp);
		this.name = tp.getName();
	}

	public ExprCode(String name, Code... args) {
		super(args);
		this.name = name;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushCall(env, this);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			// final Code[] params = this.args;
			List<Template> l = new ArrayList<>(8);
			env.findList(this.name, Template.class, l,
					(tt) -> !tt.isExpired() && tt.getParamSize() == this.args.length);
			if (l.size() == 0) {
				env.findList(this.name, Template.class, l, (tt) -> !tt.isExpired());
				throw new ErrorCode("undefined %s%s%s", this.name, this.msgArgs(), this.msgHint(l));
			}
			if (l.size() == 1) {
				return this.setTemplateAsType(env, l.get(0).update(env, this.args), ret);
			}
			this.typeArgs(env, l);
			Template selected = l.get(0);
			int mapCost = this.checkMapCost(env, ret, selected);
			ODebug.trace("cost=%d,%s", mapCost, selected);
			for (int i = 1; i < l.size(); i++) {
				if (mapCost > 0) {
					Template next = l.get(i);
					int nextCost = this.checkMapCost(env, ret, next);
					ODebug.trace("nextcost=%d,%s", nextCost, next);
					if (nextCost < mapCost) {
						mapCost = nextCost;
						selected = next;
					}
				}
			}
			if (mapCost >= CastCode.STUPID) {
				throw new ErrorCode("mismatched %s%s%s", this.name, this.msgArgs(), this.msgHint(l));
			}
			return this.setTemplateAsType(env, selected.update(env, this.args), ret);
		}
		return super.castType(env, ret);
	}

	protected void typeArgs(TEnv env, List<Template> l) {
		for (int i = 0; i < this.args.length; i++) {
			Ty pt = this.getCommonParamType(l, i);
			// ODebug.trace("common[%d] %s", i, pt);
			this.args[i] = this.args[i].asType(env, pt);
		}
	}

	private Ty getCommonParamType(List<Template> l, int n) {
		Ty ty = l.get(0).getParamTypes()[n];
		// ODebug.trace("DD %s", l);
		for (int i = 1; i < l.size(); i++) {
			if (!ty.eq(l.get(i).getParamTypes()[n])) {
				return Ty.tUntyped();
			}
		}
		return ty;
	}

	private String msgArgs() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		StringCombinator.joins(sb, this.args, ", ", p -> p.getType());
		sb.append(")");
		return sb.toString();
	}

	private String msgHint(List<Template> l) {
		StringBuilder sb = new StringBuilder();
		StringCombinator.joins(sb, l, ", ", tp -> tp.getName() + ": " + tp.getFuncType());
		if (sb.length() == 0) {
			return "";
		}
		return " \t" + TFmt.hint + " " + sb;
	}

	private int checkMapCost(TEnv env, Ty ret, Template tp) {
		int mapCost = 0;
		VarDomain dom = null;
		Ty[] p = tp.getParamTypes();
		Ty codeRet = tp.getReturnType();
		if (tp.isGeneric()) {
			dom = new VarDomain(p.length + 1);
			Ty[] gp = new Ty[p.length];
			for (int i = 0; i < p.length; i++) {
				gp[i] = p[i].dupTy(dom);
			}
			p = gp;
			codeRet = codeRet.dupTy(dom);
		}
		for (int i = 0; i < this.args.length; i++) {
			mapCost += env.mapCost(env, this.args[i].getType(), p[i]);
			if (mapCost >= CastCode.STUPID) {
				return mapCost;
			}
		}
		if (ret.isSpecific()) {
			mapCost += env.mapCost(env, codeRet, ret);
		}
		if (dom != null) {
			mapCost += dom.mapCost();
		}
		return mapCost;
	}

	private Code setTemplateAsType(TEnv env, Template tp, Ty t) {
		Ty[] p = tp.getParamTypes();
		Ty ret = tp.getReturnType();
		if (tp.isGeneric()) {
			VarDomain dom = new VarDomain(p.length + 1);
			Ty[] gp = new Ty[p.length];
			for (int i = 0; i < p.length; i++) {
				gp[i] = p[i].dupTy(dom);
			}
			ret = ret.dupTy(dom);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, gp[i]);
				if (p[i] instanceof VarTy) {
					ODebug.trace("must upcast %s => %s", p[i], gp[i]);
					this.args[i] = new TBoxCode(gp[i], this.args[i]);
				}
			}
			this.setTemplate(tp);
			this.setType(ret);
			Code result = this;
			if (tp.getReturnType() instanceof VarTy) {
				ODebug.trace("must downcast %s => %s", tp.getReturnType(), ret);
				result = new TUnboxCode(ret, result);
			}
			return result.castType(env, t);
		} else {
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, p[i]);
			}
			this.setTemplate(tp);
			this.setType(ret);
			return this.castType(env, t);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 1) {
				sb.append(" ");
			}
			StringCombinator.append(sb, this.args[i]);
		}
		sb.append(")");
	}

}