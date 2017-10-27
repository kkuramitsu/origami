package blue.origami.transpiler.target;

import blue.origami.common.OArrays;
import blue.origami.transpiler.type.Ty;

class SourceParamCode implements /* FuncParam, */ SourceEmitter {
	final SyntaxMapper syntax;
	final int startIndex;
	final String[] paramNames;
	final Ty[] paramTypes;

	SourceParamCode(SyntaxMapper syntax, int startIndex, String[] paramNames, Ty[] paramTypes) {
		this.syntax = syntax;
		this.startIndex = startIndex;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
	}

	SourceParamCode(SyntaxMapper syntax, Ty[] paramTypes) {
		this(syntax, 0, OArrays.emptyNames, paramTypes);
	}

	// @Override
	public int getStartIndex() {
		return this.startIndex;
	}

	// @Override
	public String[] getParamNames() {
		return this.paramNames;
	}

	// @Override
	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	public String getNameAt(int index) {
		if (this.getParamNames().length == 0) {
			return String.valueOf((char) ('a' + index));
		}
		return this.getParamNames()[index] + (this.getStartIndex() + index);
	}

	public int size() {
		return this.getParamTypes().length;
	}

	@Override
	public void emit(SourceSection sec) {
		String delim = this.syntax.symbol("paramdelim", ",", ",");
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sec.push(delim);
			}
			sec.pushf(this.syntax.fmt("param", "%1$s %2$s"), this.getParamTypes()[i], this.getNameAt(i));
		}
	}

}