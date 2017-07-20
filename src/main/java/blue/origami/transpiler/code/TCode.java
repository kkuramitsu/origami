package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TInst;
import blue.origami.transpiler.TSkeleton;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCastCode.TConvTemplate;

public interface TCode extends TCodeAPI {
	public TType getType();

	public TCode setSourcePosition(Tree<?> t);

	public TSkeleton getTemplate(TEnv env);

	public String strOut(TEnv env);

	public void emitCode(TEnv env, TCodeSection sec);
}

interface TCodeAPI {
	TCode self();

	public default TType refineType(TEnv env, TType t) {
		return self().getType();
	}

	public default TCode asType(TEnv env, TType t) {
		TCode self = self();
		TType f = this.refineType(env, t);
		if (t.equals(f)) {
			return self;
		}
		TConvTemplate tt = env.findTypeMap(env, f, t);
		return new TCastCode(t, tt, self);
	}

	public default TCode applyCode(TEnv env, TCode... params) {
		return self();
	}

	public default TCode op(TEnv env, String op, TCode right) {
		return self();
	}

}

abstract class TTypedCode implements TCode {
	private TType typed;

	TTypedCode(TType typed) {
		this.setType(typed);
	}

	@Override
	public TCode self() {
		return this;
	}

	@Override
	public TType getType() {
		return this.typed;
	}

	public void setType(TType typed) {
		assert (typed != null);
		this.typed = typed;
	}

	@Override
	public abstract TSkeleton getTemplate(TEnv env);

	@Override
	public abstract String strOut(TEnv env);

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		TInst[] insts = this.getTemplate(env).getInsts();
		if (insts.length > 0) {
			for (TInst inst : insts) {
				inst.emit(env, this, sec);
			}
		} else {
			sec.push(this.strOut(env));
		}
	}

	@Override
	public TCode setSourcePosition(Tree<?> t) {
		return this;
	}
}

class TArgCode extends TTypedCode {
	protected TSkeleton template;
	protected TCode[] args;

	TArgCode(TType t, TSkeleton template, TCode... args) {
		super(t);
		this.template = template;
		this.args = args;
	}

	@Override
	public TSkeleton getTemplate(TEnv env) {
		return this.template;
	}

	@Override
	public String strOut(TEnv env) {
		switch (this.args.length) {
		case 0:
			return this.getTemplate(env).format();
		case 1:
			return this.getTemplate(env).format(this.args[0].strOut(env));
		case 2:
			return this.getTemplate(env).format(this.args[0].strOut(env), this.args[1].strOut(env));
		default:
			Object[] p = new String[this.args.length];
			for (int i = 0; i < this.args.length; i++) {
				p[i] = this.args[i].strOut(env);
			}
			return this.getTemplate(env).format(p);
		}
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.push(this.strOut(env));
	}

}
