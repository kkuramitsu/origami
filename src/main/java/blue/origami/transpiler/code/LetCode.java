package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarTy;
import origami.nez2.Token;

public class LetCode extends Code1 {
	public boolean isImplicit = false;
	public Ty declType;
	public String name;
	public int index = -1;

	public LetCode(Token ns, Ty type, Code expr) {
		super(expr);
		this.setSource(ns);
		this.name = ns.getSymbol();
		if (type != null) {
			this.declType = NameHint.isMutable(this.name) ? type.toMutable() : type;
		} else {
			VarTy varTy = Ty.tVar(ns);
			this.declType = varTy;
		}
	}

	public LetCode(String name, Code expr) {
		this(NameHint.getName(name), null, expr);
	}

	public boolean isMutable() {
		return false;
	}

	public LetCode asImplicit() {
		this.isImplicit = true;
		return this;
	}

	public String getName() {
		return this.index == -1 ? this.name : this.name + this.index;
	}

	public Ty getDeclType() {
		return this.declType;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		return env.getLanguage().typeLet(env, this, ret);
	}

	public Code defineAsGlobal(Env env, boolean isPublic) {
		Code right = this.getInner();
		try {
			right = right.bindAs(env, this.getSource(), this.declType);
			if (!NameHint.isMutable(this.name)) {
				this.declType = this.declType.toImmutable();
			}
			if (!right.showError(env)) {
				Transpiler tp = env.getTranspiler();
				CodeMap defined = tp.defineConst(isPublic, this.name, this.declType, right);
				env.add(this.name, defined);
			}
		} catch (ErrorCode e) {
			e.showError(env);
		}
		return new DoneCode();
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushLet(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "let " + this.getName(), this.getInner());
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Keyword("let ");
		sh.TypeAnnotation_(this.getDeclType(), () -> {
			sh.Name(this.name);
		});
		sh.Operator(" = ");
		sh.Expr(this.getInner());
	}

}
