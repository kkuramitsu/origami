package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TUnaryCode extends TTypedCode {
	protected TTemplate template;
	protected TCode inner;

	public TUnaryCode(TType ret, TTemplate template, TCode inner) {
		super(ret);
		this.template = template;
		this.inner = inner;
	}

	@Override
	public TTemplate getTemplate(TEnv env) {
		return this.template;
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.inner.strOut(env));
	}
}