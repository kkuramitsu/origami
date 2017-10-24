package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeMap;

public interface MappedCode extends Code {
	public CodeMap getMapped();

	public default boolean hasTemplate() {
		return this.getMapped() != null;
	}

}
