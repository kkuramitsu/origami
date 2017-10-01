package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.FunctionContext.Variable;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.OStrings;

public class LetCode extends Code1 {
	private boolean isImplicit = false;
	private Ty declType;
	private String name;
	private int index = -1;

	public LetCode(String name, Code expr) {
		this(name, null, expr);
	}

	public LetCode(String name, Ty type, Code expr) {
		super(expr);
		this.name = name;
		this.declType = type == null ? Ty.tUntyped() : type;
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

	// public boolean isDuplicated() {
	// return this.isDuplicated;
	// }

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			FunctionContext fcx = env.get(FunctionContext.class);
			if (fcx == null) {
				fcx = new FunctionContext(null); // TopLevel
				env.add(FunctionContext.class, fcx);
			}
			if (this.isImplicit) {

			}
			Variable var = fcx.newVariable(this.name, this.index, this.declType);
			env.add(this.name, var);
			this.index = var.getIndex();

			this.inner = this.inner.bind(this.declType).asType(env, this.declType);
			ODebug.trace("let %s %s %s", this.name, this.declType, this.inner.getType());
			this.setType(Ty.tVoid);
			;
			// if () {
			// this.isDuplicated = true;
			// ODebug.trace("duplicated local name %s", this.name);
			// }
		}
		return this;
	}

	public Code defineAsGlobal(TEnv env, boolean isPublic) {
		Code right = this.getInner();
		try {
			if (this.declType == null) {
				this.declType = Ty.tUntyped();
				right = right.bind(this.declType).asType(env, this.declType);
				this.declType = right.getType();
			} else {
				right = right.bind(this.declType).asType(env, this.declType);
			}
			if (!right.showError(env)) {
				Transpiler tp = env.getTranspiler();
				CodeMap defined = tp.defineConst(isPublic, this.name, this.declType, right);
				env.add(this.name, defined);
			}
		} catch (ErrorCode e) {
			e.showError(env);
		}
		return new DeclCode();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushLet(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("let ");
		sb.append(this.name);
		sb.append(" = ");
		OStrings.append(sb, this.getInner());
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.Keyword("let ");
		sh.TypeAnnotation_(this.getDeclType(), () -> {
			sh.Name(this.name);
		});
		sh.Operator(" = ");
		sh.Expr(this.getInner());
	}

}
