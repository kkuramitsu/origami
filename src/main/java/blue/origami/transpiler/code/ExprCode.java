package blue.origami.transpiler.code;

import java.util.Arrays;
import java.util.List;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.CastCode.BoxCastCode;
import blue.origami.transpiler.code.CastCode.FuncCastCode;
import blue.origami.transpiler.code.CastCode.UnboxCastCode;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.transpiler.type.VarTy;
import blue.origami.util.ODebug;
import blue.origami.util.OStrings;

public class ExprCode extends CodeN implements CallCode {

	protected String name;
	private Template tp;

	public ExprCode(String name, Code... args) {
		super(args);
		this.name = name;
		this.tp = null;
	}

	public ExprCode(Template tp, Code... args) {
		super(tp.getReturnType(), args);
		this.name = tp.getName();
		this.setTemplate(tp);
	}

	@Override
	public Template getTemplate() {
		assert (this.tp != null);
		return this.tp;
	}

	public void setTemplate(Template tp) {
		this.tp = tp;
	}

	@Override
	public boolean isAbstract() {
		return this.tp.isAbstract();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushCall(env, this);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			List<Template> founds = env.findTemplates(this.name, this.args.length);
			this.typeArgs(env, founds);
			Ty[] p = Arrays.stream(this.args).map(c -> c.getType()).toArray(Ty[]::new);
			// ODebug.trace("founds=%s", founds);
			if (founds.size() == 0) {
				return this.asUnfound(env, founds).castType(env, ret);
			}
			if (founds.size() == 1) {
				return this.asMatched(env, founds.get(0).generate(env, p), ret);
			}
			this.typeArgs(env, founds);
			Template selected = Template.select(env, founds, ret, p, this.maxCost());
			if (selected == null) {
				return this.asMismatched(env, founds).castType(env, ret);
			}
			return this.asMatched(env, selected.generate(env, p), ret);
		}
		return super.castType(env, ret);
	}

	public int maxCost() {
		return CastCode.BADCONV;
	}

	protected Code asUnfound(TEnv env, List<Template> l) {
		env.findList(this.name, Template.class, l, (tt) -> !tt.isExpired());
		throw new ErrorCode(this, TFmt.undefined_SSS, this.name, this.msgArgs(), msgHint(env, l));
	}

	protected Code asMismatched(TEnv env, List<Template> l) {
		if (l.get(0).isMutation() && !this.args[0].getType().isMutable()) {
			throw new ErrorCode(this, TFmt.not_mutable_SSS, this.name, this.msgArgs(), msgHint(env, l));
		}
		throw new ErrorCode(this, TFmt.mismatched_SSS, this.name, this.msgArgs(), msgHint(env, l));
	}

	protected void typeArgs(TEnv env, List<Template> l) {
		for (int i = 0; i < this.args.length; i++) {
			Ty pt = this.getCommonParamType(l, i);
			// ODebug.trace("common[%d] %s", i, pt);
			this.args[i] = this.args[i].asType(env, pt);
			// ODebug.trace("typed[%d] %s %s", i, this.args[i],
			// this.args[i].getType());
		}
	}

	private Ty getCommonParamType(List<Template> l, int n) {
		// Ty ty = l.get(0).getParamTypes()[n];
		// ODebug.trace("DD %s", l);
		// for (int i = 1; i < l.size(); i++) {
		// if (!ty.eq(l.get(i).getParamTypes()[n])) {
		return Ty.tUntyped();
		// }
		// }
		// return ty;
	}

	private String msgArgs() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		OStrings.joins(sb, this.args, ", ", p -> p.getType().finalTy());
		sb.append(")");
		return sb.toString();
	}

	static String msgHint(TEnv env, List<Template> l) {
		StringBuilder sb = new StringBuilder();
		OStrings.joins(sb, l, ", ", tp -> tp.getName() + ": " + tp.getFuncType());
		if (sb.length() == 0) {
			return "";
		}
		return " \t" + TFmt.hint + " " + sb;
	}

	private Code asMatched(TEnv env, Template defined, Ty t) {
		Ty[] dpats = defined.getParamTypes();
		Ty dret = defined.getReturnType();
		if (defined.isGeneric()) {
			VarDomain dom = new VarDomain(dpats);
			Ty[] gParamTypes = dom.dupParamTypes(dpats, null);
			Ty dRetType = dom.dupRetType(dret);
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, gParamTypes[i]);
				if (!defined.isAbstract()) {
					if (dpats[i] instanceof VarTy) {
						ODebug.trace("MUST upcast %s => %s", gParamTypes[i], gParamTypes[i]);
						this.args[i] = new BoxCastCode(gParamTypes[i], this.args[i]);
					}
					if (dpats[i] instanceof FuncTy && dpats[i].hasVar()) {
						Ty anyTy = dpats[i].dupVar(null); // AnyRef
						Template conv = env.findTypeMap(env, gParamTypes[i].finalTy(), anyTy.finalTy());
						ODebug.trace("MUST funccast %s => %s :: %s", gParamTypes[i], anyTy, conv);
						this.args[i] = new FuncCastCode(anyTy, conv, this.args[i]);
					}
				}
			}
			if (defined.isMutation()) {
				this.args[0].getType().hasMutation(true);
			}
			this.setTemplate(defined);
			this.setType(dRetType);
			Code result = this;
			if (!defined.isAbstract()) {
				if (defined.getReturnType() instanceof VarTy) {
					ODebug.trace("must downcast %s => %s", defined.getReturnType(), dRetType);
					result = new UnboxCastCode(dRetType, result);
				}
				if (defined.getReturnType() instanceof FuncTy && defined.getReturnType().hasVar()) {
					Ty anyTy = defined.getReturnType().dupVar(null); // AnyRef
					Template conv = env.findTypeMap(env, dRetType, anyTy);
					ODebug.trace("MUST funccast %s => %s :: %s", anyTy, dRetType, conv);
					result = new FuncCastCode(dRetType, conv, result);
				}
			}
			return result.castType(env, t);
		} else {
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].asType(env, dpats[i]);
			}
			if (defined.isMutation()) {
				this.args[0].getType().hasMutation(true);
			}
			this.setTemplate(defined);
			this.setType(dret);
			return this.castType(env, t);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		sb.append(this.name);
		if (this.args.length > 0) {
			sb.append(" ");
		}
		OStrings.joins(sb, this.args, " ");
		sb.append(")");
	}

	@Override
	public void dumpCode(SyntaxHighlight sb) {
		sb.TypeAnnotation(this.getType(), () -> {
			sb.Name(this.name);
			sb.Token("(");
			for (int i = 0; i < this.args.length; i++) {
				if (i > 0) {
					sb.Token(",");
				}
				sb.Expr(this.args[i]);
			}
			sb.Token(")");
		});
	}

	public static ExprCode option(String name, Code... args) {
		return new OptionExprCode(name, args);
	}

}

class OptionExprCode extends ExprCode implements CallCode {

	OptionExprCode(String name, Code... code) {
		super(name, code);
	}

	@Override
	public int maxCost() {
		return CastCode.CAST;
	}

	@Override
	protected Code asUnfound(TEnv env, List<Template> l) {
		return this.args[0];
	}

	@Override
	protected Code asMismatched(TEnv env, List<Template> l) {
		return this.args[0];
	}

}
