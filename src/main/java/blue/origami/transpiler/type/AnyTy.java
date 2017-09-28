package blue.origami.transpiler.type;

import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.CastCode;

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
	public CodeMap findMapTo(TEnv env, Ty toTy) {
		String format = env.getSymbol("cast", "(%s)%s");
		return new CodeMap(CastCode.BESTCAST, "anycast", format, this, toTy);
	}

	@Override
	public int costMapFrom(TEnv env, Ty fromTy) {
		return CastCode.BESTCAST;
	}

	@Override
	public CodeMap findMapFrom(TEnv env, Ty fromTy) {
		String format = env.getSymbol("upcast", "%s");
		return new CodeMap(CastCode.BESTCAST, "upcast", format, fromTy, this);
	}

}