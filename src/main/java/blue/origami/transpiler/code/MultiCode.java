package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class MultiCode extends CodeN {

	public MultiCode(Code... nodes) {
		super(AutoType, nodes);
	}

	public MultiCode() {
		this(TArrays.emptyCodes);
	}

	public MultiCode(List<Code> codes) {
		super(AutoType, codes.toArray(new Code[codes.size()]));
	}

	// private boolean isBlockExpr;
	//
	// public boolean isBlockExpr() {
	// return this.isBlockExpr;
	// }
	//
	// public void setBlockExpr(boolean isBlockExpr) {
	// this.isBlockExpr = isBlockExpr;
	// }

	@Override
	public boolean isEmpty() {
		for (Code a : this.args) {
			if (!a.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Ty getType() {
		if (this.args.length == 0) {
			return Ty.tVoid;
		}
		return this.args[this.args.length - 1].getType();
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.args.length > 0) {
			TEnv lenv = env.newEnv();
			final int last = this.args.length - 1;
			for (int i = 0; i < last; i++) {
				final int n = i;
				this.args[i] = env.catchCode(() -> this.args[n].asType(lenv, Ty.tVoid));
			}
			this.args[last] = env.catchCode(() -> this.args[last].asType(lenv, ret));
		}
		return this;
	}

	@Override
	public Code addReturn() {
		int last = this.args.length - 1;
		this.args[last] = this.args[last].addReturn();
		return this;
	}

	@Override
	public boolean hasReturn() {
		return this.args[this.args.length - 1].hasReturn();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushMulti(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("{");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 1) {
				sb.append(";");
			}
			StringCombinator.append(sb, this.args[i]);
		}
		sb.append("}");
	}

}
