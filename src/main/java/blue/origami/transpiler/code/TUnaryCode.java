package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TSkeleton;
import blue.origami.transpiler.TType;

public abstract class TUnaryCode extends TStaticAtomCode {
	protected TSkeleton template;
	protected TCode inner;

	public TUnaryCode(TType ret, TSkeleton template, TCode inner) {
		super(ret);
		this.template = template;
		this.inner = inner;
	}

	@Override
	public TSkeleton getTemplate(TEnv env) {
		return this.template;
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.inner.strOut(env));
	}

}