package blue.origami.lang;

import blue.origami.nez.ast.SourcePosition;
import blue.origami.util.Handled;
import blue.origami.util.OStackable;

public class OEnvEntry implements OStackable<OEnvEntry>, Handled<Object> {
	private Object value;
	private OEnvEntry onstack = null;

	OEnvEntry(SourcePosition s, Object value) {
		this.value = value;
	}

	@Override
	public OEnvEntry push(OEnvEntry onstack) {
		this.onstack = onstack;
		return this;
	}

	@Override
	public OEnvEntry pop() {
		return this.onstack;
	}

	@Override
	public Object getHandled() {
		return this.value;
	}

	public Object setHandled(Object value) {
		Object v = this.value;
		this.value = value;
		return v;
	}
}