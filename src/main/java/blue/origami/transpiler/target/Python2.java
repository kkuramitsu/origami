package blue.origami.transpiler.target;

import blue.origami.transpiler.CodeMapper;
import blue.origami.transpiler.Transpiler;

public class Python2 extends Transpiler {

	/* Don't create constructor */
	// public Python2(Grammar g, Parser p) {
	// super(g, p);
	// }

	@Override
	public CodeMapper getCodeMapper() {
		return new PythonMapper(this);
	}

	static class PythonMapper extends SourceMapper {
		public PythonMapper(Transpiler tr) {
			super(tr, new PythonTypeMapper(tr));
		}

		@Override
		public SourceSection newSourceSection() {
			return new PythonSection(this.syntax, this.ts);
		}

	}

	static class PythonTypeMapper extends SourceTypeMapper {
		public PythonTypeMapper(Transpiler env) {
			super(env);
		}

	}

	static class PythonSection extends SourceSection {
		public PythonSection(SyntaxMapper syntax, SourceTypeMapper ts) {
			super(syntax, ts);
		}
	}

}
