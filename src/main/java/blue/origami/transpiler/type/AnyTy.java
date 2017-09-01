package blue.origami.transpiler.type;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CastCode.TConvTemplate;

class AnyTy extends SimpleTy {

	AnyTy() {
		super("AnyRef");
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		return codeTy == this;
	}

	@Override
	public int costMapTo(TEnv env, Ty toTy) {
		return CastCode.BESTCAST;
	}

	@Override
	public Template findMapTo(TEnv env, Ty toTy) {
		String format = env.getSymbol("cast", "(%s)%s");
		return new TConvTemplate("", this, toTy, CastCode.BESTCAST, format);
	}

	@Override
	public int costMapFrom(TEnv env, Ty fromTy) {
		return CastCode.BESTCAST;
	}

	@Override
	public Template findMapFrom(TEnv env, Ty fromTy) {
		String format = env.getSymbol("upcast", "%s");
		return new TConvTemplate("", fromTy, this, CastCode.BESTCAST, format);
	}

}