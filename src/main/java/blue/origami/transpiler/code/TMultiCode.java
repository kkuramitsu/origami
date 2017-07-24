package blue.origami.transpiler.code;

import blue.origami.transpiler.SourceSection;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TMultiCode extends MultiCode {

	private boolean isBlockExpr;

	public TMultiCode(boolean isBlockExpr, TCode... nodes) {
		super(nodes);
		this.setBlockExpr(isBlockExpr);
	}

	public boolean isBlockExpr() {
		return this.isBlockExpr;
	}

	public void setBlockExpr(boolean isBlockExpr) {
		this.isBlockExpr = isBlockExpr;
	}

	@Override
	public TType getType() {
		return this.args[this.args.length - 1].getType();
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		final int last = this.args.length - 1;
		for (int i = 0; i < last; i++) {
			this.args[i] = this.args[i].asType(env, TType.tVoid);
		}
		this.args[last] = this.args[last].asType(env, t);
		return this;
	}

	@Override
	public boolean hasReturn() {
		return this.args[this.args.length - 1].hasReturn();
	}

	@Override
	public TCode addReturn() {
		int last = this.args.length - 1;
		this.args[last] = this.args[last].addReturn();
		return this;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return Template.Null;
	}

	@Override
	public String strOut(TEnv env) {
		SourceSection sec = env.newSourceSection();
		sec.pushMulti(env, this);
		return sec.toString();
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushMulti(env, this);
	}

}
