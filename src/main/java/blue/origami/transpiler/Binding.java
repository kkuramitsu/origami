package blue.origami.transpiler;

import blue.origami.common.Handled;
import blue.origami.common.SourcePosition;

class Binding implements Handled<Object> {
	private Object value;
	private Binding onstack = null;

	Binding(SourcePosition s, Object value) {
		this.value = value;
	}

	public Binding push(Binding onstack) {
		this.onstack = onstack;
		return this;
	}

	public Binding pop() {
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