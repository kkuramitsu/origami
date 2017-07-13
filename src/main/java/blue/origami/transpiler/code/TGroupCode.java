package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TGroupCode implements TCode {
	protected TCode inner;

	TGroupCode(TCode inner) {
		this.inner = inner;
	}

	@Override
	public TCode self() {
		return this;
	}

	@Override
	public TType getType() {
		return this.inner.getType();
	}

	@Override
	public TCode setSourcePosition(Tree<?> t) {
		return this.inner.setSourcePosition(t);
	}

	@Override
	public TTemplate getTemplate(TEnv env) {
		TTemplate t = env.get("()", TTemplate.class);
		return (t == null) ? TTemplate.Null : t;
	}

	@Override
	public String strOut(TEnv env) {
		TTemplate t = env.get("()", TTemplate.class);
		if (t == null) {
			return this.inner.strOut(env);
		}
		return t.format(this.inner.strOut(env));
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.push(this.strOut(env));
	}
}
