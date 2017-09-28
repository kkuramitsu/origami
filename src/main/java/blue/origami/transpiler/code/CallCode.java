package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeMap;

public interface CallCode extends Code {
	public CodeMap getTemplate();

	public default boolean hasTemplate() {
		return this.getTemplate() != null;
	}

}
