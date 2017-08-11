package blue.origami.transpiler.code;

import blue.origami.transpiler.Template;

public interface CallCode extends Code {
	public Template getTemplate();

	public default boolean hasTemplate() {
		return this.getTemplate() != null;
	}
}
