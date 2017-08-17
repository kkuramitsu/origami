package blue.origami.transpiler.code;

import java.util.List;

public interface CodeBuilder {

	public default Code v(boolean b) {
		return new BoolCode(b);
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

	public default Code get(Code c) {
		return new ExprCode("getv", c);
	}

	public default Code isSome(Code c) {
		return new ExprCode("isSome", c);
	}

	public default Code isNull(Code c) {
		return new ExprCode("isNone", c);
	}

}
