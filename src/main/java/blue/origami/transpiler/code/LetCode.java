package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.type.Ty;

public class LetCode extends Code1 {
	public boolean isImplicit = false;
	public Ty declType;
	public String name;
	public int index = -1;

	public LetCode(AST name, Ty type, Code expr) {
		super(expr);
		this.setSource(name);
		this.name = name.getString();
		this.declType = type == null ? Ty.tVar(name) : type;
	}

	public LetCode(String name, Code expr) {
		this(AST.getName(name), null, expr);
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

	public void toMutableType() {
		if (this.name.endsWith("$")) {
			this.declType = this.declType.toMutable();
		}
	}

	@Override
	public Code asType(Env env, Ty ret) {
		return env.getLanguage().typeLet(env, this, ret);
	}

	public Code defineAsGlobal(Env env, boolean isPublic) {
		Code right = this.getInner();
		try {
			right = right.bindAs(env, this.declType);
			this.toMutableType();
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
