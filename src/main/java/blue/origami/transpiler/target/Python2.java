package blue.origami.transpiler.target;

import blue.origami.transpiler.CodeMapper;
import blue.origami.transpiler.Transpiler;

public class Python2 extends Transpiler {
	@Override
	public CodeMapper getCodeMapper() {
		return new PythonMapper(this);
	}

	static class PythonMapper extends SourceMapper {
		public PythonMapper(Transpiler tr) {
			super(tr, new PythonTypeMapper(tr));
		}
	}

	static class PythonTypeMapper extends SourceTypeMapper {
		public PythonTypeMapper(Transpiler env) {
			super(env);
		}

	}
}
