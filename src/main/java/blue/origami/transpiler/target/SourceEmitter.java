package blue.origami.transpiler.target;

import blue.origami.transpiler.Env;

public interface SourceEmitter {
	public void emit(Env env, SourceSection sec);
}
