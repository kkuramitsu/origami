package blue.origami.transpiler.code;

import blue.origami.transpiler.SourceSection;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TMultiCode extends CodeN {

	private boolean isBlockExpr;

	public TMultiCode(boolean isBlockExpr, TCode... nodes) {
		super(nodes);
		this.setBlockExpr(isBlockExpr);
	}

	public TMultiCode() {
		this(false, TArrays.emptyCodes);
	}

	public boolean isBlockExpr() {
		return this.isBlockExpr;
	}

	public void setBlockExpr(boolean isBlockExpr) {
		this.isBlockExpr = isBlockExpr;
	}

	@Override
	public boolean isEmpty() {
		for (TCode a : this.args) {
			if (!a.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public TType getType() {
		if (this.args.length == 0) {
			return TType.tVoid;
		}
		return this.args[this.args.length - 1].getType();
	}

	@Override
	public TCode asType(TEnv env, TType ret) {
		if (this.args.length > 0) {
			final int last = this.args.length - 1;
			for (int i = 0; i < last; i++) {
				final int n = i;
				this.args[i] = env.catchCode(() -> this.args[n].asType(env, TType.tVoid));
			}
			this.args[last] = env.catchCode(() -> this.args[last].asType(env, ret));
		}
		return this;
	}

	@Override
	public TCode addReturn() {
		int last = this.args.length - 1;
		this.args[last] = this.args[last].addReturn();
		return this;
	}

	@Override
	public boolean hasReturn() {
		return this.args[this.args.length - 1].hasReturn();
	}

	@Override
	public Template getTemplate(TEnv env) {
		return Template.Null;
	}

	@Override
	public String strOut(TEnv env) {
		SourceSection p = env.getCurrentSourceSection();
		SourceSection sec = p.dup();
		env.setCurrentSourceSection(sec);
		sec.pushMulti(env, this);
		env.setCurrentSourceSection(p);
		return sec.toString();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushMulti(env, this);
	}

}
