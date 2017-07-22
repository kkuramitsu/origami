package blue.origami.transpiler.code;

import java.util.Iterator;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.EmptyCode.EmptyCodeIterator;

public class TGroupCode extends SingleCode {
	protected TCode inner;

	TGroupCode(TCode inner) {
		super(inner);
	}

	@Override
	public TCode self() {
		return this;
	}

	@Override
	public Iterator<TCode> iterator() {
		return new EmptyCodeIterator();
	}

	@Override
	public Template getTemplate(TEnv env) {
		Template t = env.get("()", Template.class);
		return (t == null) ? Template.Null : t;
	}

	@Override
	public String strOut(TEnv env) {
		Template t = env.get("()", Template.class);
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
