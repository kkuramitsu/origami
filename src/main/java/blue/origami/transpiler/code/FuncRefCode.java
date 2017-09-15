package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;

public final class FuncRefCode extends CommonCode {
	private String name;
	private Template template;

	public FuncRefCode(String name, Template tp) {
		super(tp.getFuncType());
		this.name = name;
		this.template = tp;
	}

	public String getName() {
		return this.name;
	}

	public Template getRef() {
		return this.template;
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (ret.isFunc()) {
			FuncTy funcTy = (FuncTy) ret.real();
			List<Template> l = env.findTemplates(this.name, funcTy.getParamSize());
			if (l.size() == 0) {
				return this.asUnfound(env, l, funcTy);
			}
			if (l.size() == 1) {
				return this.asMatched(env, l.get(0).generate(env, funcTy.getParamTypes()), ret);
			}
			Template selected = Template.select(env, l, funcTy.getReturnType(), funcTy.getParamTypes(), 0);
			if (selected == null) {
				return this.asMismatched(env, l, funcTy);
			}
			return this.asMatched(env, selected.generate(env, funcTy.getParamTypes()), ret);
		}
		if (this.isUntyped()) {
			this.template.used(env);
			this.setType(this.template.getFuncType());
		}
		return super.castType(env, ret);
	}

	@Override
	public boolean showError(TEnv env) {
		if (this.template.isAbstract()) {
			env.reportError(this.getSource(), TFmt.abstract_function_YY1__YY2, this.name, this.template.getFuncType());
			return true;
		}
		return false;
	}

	private Code asMatched(TEnv env, Template selected, Ty ret) {
		this.template = selected;
		this.setType(selected.getFuncType());
		return this.castType(env, ret);
	}

	private Code asUnfound(TEnv env, List<Template> l, FuncTy funcTy) {
		env.findList(this.name, Template.class, l, (tt) -> !tt.isExpired());
		throw new ErrorCode(this, TFmt.undefined_SSS, this.name, "", ExprCode.msgHint(env, l));
	}

	private Code asMismatched(TEnv env, List<Template> l, FuncTy funcTy) {
		throw new ErrorCode(this, TFmt.mismatched_SSS, this.name, "", ExprCode.msgHint(env, l));
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushFuncRef(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.TypeAnnotation(this.getType(), () -> {
			sh.Name(this.name);
		});
	}

}