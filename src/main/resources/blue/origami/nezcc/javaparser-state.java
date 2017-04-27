/* Symbol Table */

static class SymbolTable {
	String label;
	String value;
	int ivalue;  // counter
	SymbolTable prev;

	SymbolTable(String label, String value, int ivalue, SymbolTable prev) {
		this.label = label;
		this.value = value;
		this.ivalue = 0;
		this.prev = prev;
	}

	SymbolTable(String label, String value, SymbolTable prev) {
		this(label, value, 0, prev);
	}
}

public interface SymbolAction {
	public void mutate(NezParserContext<?> px, String label, int ppos, Object thunk);
}

public interface SymbolPredicate {
	public boolean match(NezParserContext<?> px, String label, int ppos, Object thunk);
}

static <T> boolean callSymbolAction(NezParserContext<T> px, SymbolAction f, String label, int ppos, Object thunk) {
	f.mutate(px, label, ppos, thunk);
	return true;
}

static <T> boolean callSymbolPredicate(NezParserContext<T> px, SymbolPredicate f, String label, int ppos, Object thunk) {
	return f.match(px, label, ppos, thunk);
}

static <T> void symbol(NezParserContext<T> px, String label, int ppos, Object thunk) {
	String matched = (ppos == -1) ? "" : new String(px.inputs, ppos, px.pos);
	px.state = new SymbolTable(label, matched, (SymbolTable)px.state);
}

private static SymbolTable remove(SymbolTable st, String label) {
	if(st == null) return null;
	if(st.label.equals(label)) {
		return remove(st.prev, label);
	}
	return new SymbolTable(st.label, st.value, st.ivalue, remove(st.prev, label));
}

static <T> void reset(NezParserContext<T> px, String label, int ppos) {
	px.state = remove((SymbolTable)px.state, label);
}

static <T> boolean exists(NezParserContext<T> px, String label, int ppos, Object thunk) {
	if(thunk == null) {
		for (SymbolTable st = (SymbolTable)px.state; st != null; st = st.prev) {
			if (label.equals(st.label)) {
				return true;
			}
		}
	}
	else {
		for (SymbolTable st = (SymbolTable)px.state; st != null; st = st.prev) {
			if (label.equals(st.label) && st.value.equals(thunk)) {
				return true;
			}
		}
	}
	return false; // TODO
}

static <T> boolean match(NezParserContext<T> px, String label, int ppos, Object thunk) {
	for (SymbolTable st = (SymbolTable)px.state; st != null; st = st.prev) {
		if (label.equals(st.label)) {
			return px.matchBytes(st.value.getBytes());
		}
	}
	return false;
}

static <T> boolean equals(NezParserContext<T> px, String label, int ppos, Object thunk) {
	String matched = (ppos == -1) ? "" : new String(px.inputs, ppos, px.pos);
	for (SymbolTable st = (SymbolTable)px.state; st != null; st = st.prev) {
		if (label.equals(st.label)) {
			return matched.equals(st.value);
		}
	}
	return false;
}

static <T> boolean contains(NezParserContext<T> px, String label, int ppos, Object thunk) {
	String matched = (ppos == -1) ? "" : new String(px.inputs, ppos, px.pos);
	for (SymbolTable st = (SymbolTable)px.state; st != null; st = st.prev) {
		if (label.equals(st.label) && matched.equals(st.value)) {
			return true;
		}
	}
	return false; // TODO
}

static <T> void scan(NezParserContext<T> px, String label, int ppos, Object thunk) {
	String value = (ppos == -1) ? "" : new String(px.inputs, ppos, px.pos);
	int num = (int)Long.parseLong(value);
	px.state = new SymbolTable(label, value, num, (SymbolTable)px.state);
}

static <T> boolean dec(NezParserContext<?> px, String label, int ppos, Object thunk) {
	for (SymbolTable st = (SymbolTable)px.state; st != null; st = st.prev) {
		if (label.equals(st.label)) {
			return st.ivalue-- > 0;
		}
	}
	return false;
}

static <T> boolean zero(NezParserContext<?> px, String label, int ppos, Object thunk) {
	for (SymbolTable st = (SymbolTable)px.state; st != null; st = st.prev) {
		if (label.equals(st.label)) {
			return st.ivalue == 0;
		}
	}
	return false;
}
