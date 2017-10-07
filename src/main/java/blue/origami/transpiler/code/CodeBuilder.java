package blue.origami.transpiler.code;

import java.util.List;

public interface CodeBuilder {

	public default Code v(boolean b) {
		return new BoolCode(b);
	}

	public default Code v(int n) {
		return new IntCode(n);
	}

	public default Code group(Code c) {
		return groupfy(c);
	}

	public static Code groupfy(Code c) {
		if (c instanceof BinaryCode) {
			return new GroupCode(c);
		}
		return c;
	}

	public default Code not(Code c) {
		return new ExprCode("!", c);
	}

	public default Code and(Code... params) {
		if (params.length == 0) {
			return v(true);
		}
		Code right = params[params.length - 1];
		for (int i = params.length - 2; i >= 0; i--) {
			right = new BinaryCode("&&", params[i], right);
		}
		return right;
	}

	public default Code and(List<Code> l) {
		return this.and(l.toArray(new Code[l.size()]));
	}

	public default Code or(Code... params) {
		Code right = params[params.length - 1];
		for (int i = params.length - 2; i >= 0; i--) {
			right = new BinaryCode("||", params[i], right);
		}
		return right;
	}

	public default Code or(List<Code> l) {
		return this.or(l.toArray(new Code[l.size()]));
	}

	public default Code isSome(Code c) {
		return new ExprCode("Some?", c);
	}

	public default Code getSome(Code c) {
		return new ExprCode("getSome", c);
	}

	public default Code isNone(Code c) {
		return new ExprCode("None?", c);
	}

	public default Code len(Code c) {
		return new ExprCode("||", c);
	}

	public default Code geti(Code c, int index) {
		return new ExprCode("[]", c, v(index));
	}

	public default Code tail(Code c, int index) {
		return new ExprCode("tail", c, v(index));
	}

	public default Code tupleAt(Code c, int index) {
		return new TupleIndexCode(c, index);
	}

	public default Code op(Code c, String op, Code c2) {
		return new BinaryCode(op, c, c2);
	}

	public default Code op(Code c, String op, int num) {
		return op(c, op, v(num));
	}

}
