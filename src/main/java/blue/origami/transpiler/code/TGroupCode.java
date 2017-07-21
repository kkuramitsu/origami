package blue.origami.transpiler.code;

import java.util.Iterator;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TSkeleton;
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
	public Iterator<TCode> iterator() {
		return new AtomCodeIterator();
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
	public TSkeleton getTemplate(TEnv env) {
		TSkeleton t = env.get("()", TSkeleton.class);
		return (t == null) ? TSkeleton.Null : t;
	}

	@Override
	public String strOut(TEnv env) {
		TSkeleton t = env.get("()", TSkeleton.class);
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
