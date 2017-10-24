package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class MultiCode extends CodeN {

	public MultiCode(Code... nodes) {
		super(AutoType, nodes);
	}

	public MultiCode() {
		this(OArrays.emptyCodes);
	}

	public MultiCode(List<Code> codes) {
		super(AutoType, codes.toArray(new Code[codes.size()]));
	}

	@Override
	public boolean isGenerative() {
		for (Code a : this.args) {
			if (a.isGenerative()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Ty getType() {
		if (this.args.length == 0) {
			return Ty.tVoid;
		}
		return this.args[this.args.length - 1].getType();
	}

	boolean noScope = false;

	public MultiCode asNoScope() {
		this.noScope = true;
		return this;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.args.length > 0) {
			Env lenv = this.noScope ? env : env.newEnv();
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
	public void emitCode(CodeSection sec) {
		sec.pushMulti(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("{");
		OStrings.joins(sb, this.args, ";");
		sb.append("}");
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.append("{");
		sh.incIndent();
		for (Code c : this.args) {
			sh.Indent();
			c.dumpCode(sh);
		}
		sh.decIndent();
		sh.Indent();
		sh.append("}");
	}

}
