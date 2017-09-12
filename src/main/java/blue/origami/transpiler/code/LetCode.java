package blue.origami.transpiler.code;

import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.FunctionContext.Variable;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
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
			this.inner = this.inner.bind(this.declType).asType(env, this.declType);
			this.setType(Ty.tVoid);
		}
		FunctionContext fcx = env.get(FunctionContext.class);
		if (fcx == null) {
			fcx = new FunctionContext(null); // TopLevel
			env.add(FunctionContext.class, fcx);
		}
		if (this.isImplicit) {

		}
		// if () {
		// this.isDuplicated = true;
		// ODebug.trace("duplicated local name %s", this.name);
		// }
		Variable var = fcx.newVariable(this.name, this.declType);
		this.index = var.getIndex();
		// ODebug.trace("let %s %s %s", env, this.name, this.declType);
		env.add(this.name, var);
		return this;
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

}
