package blue.origami.transpiler.code;

import blue.origami.transpiler.FunctionContext;
import blue.origami.transpiler.FunctionContext.Variable;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class LetCode extends Code1 {
	private Ty declType; // nullable
	private String name;
	private boolean isDuplicated = false;

	public LetCode(String name, Ty type, Code expr) {
		super(expr);
		this.name = name;
		this.declType = type;
	}

	public String getName() {
		return this.name;
	}

	public Ty getDeclType() {
		return this.declType == null ? Ty.tUntyped() : this.declType;
	}

	public boolean isDuplicated() {
		return this.isDuplicated;
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			this.inner = this.inner.asType(env, this.declType);
			if (Ty.isUntyped(this.declType)) {
				this.declType = this.inner.guessType();
			}
			this.setType(Ty.tVoid);
		}
		FunctionContext fcx = env.get(FunctionContext.class);
		assert (fcx != null);
		if (fcx.isDuplicatedName(this.name, this.declType)) {
			this.isDuplicated = true;
			ODebug.trace("duplicated local name %s", this.name);
		}
		Variable var = fcx.newVariable(this.name, this.declType);
		env.add(this.name, var);
		return this;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushLet(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
		sb.append(" = ");
		StringCombinator.append(sb, this.getInner());
	}

}
