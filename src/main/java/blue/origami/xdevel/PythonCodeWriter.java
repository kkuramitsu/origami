package blue.origami.xdevel;

import blue.origami.code.OAndCode;
import blue.origami.code.OArrayCode;
import blue.origami.code.OAssignCode;
import blue.origami.code.OBreakCode;
import blue.origami.code.OCastCode;
import blue.origami.code.OCode;
import blue.origami.code.OConstructorCode;
import blue.origami.code.OContinueCode;
import blue.origami.code.OEmptyCode;
import blue.origami.code.OGenerator;
import blue.origami.code.OGetIndexCode;
import blue.origami.code.OGetSizeCode;
import blue.origami.code.OGetterCode;
import blue.origami.code.OIfCode;
import blue.origami.code.OInstanceOfCode;
import blue.origami.code.OLambdaCode;
import blue.origami.code.OMultiCode;
import blue.origami.code.ONameCode;
import blue.origami.code.ONotCode;
import blue.origami.code.OOrCode;
import blue.origami.code.OReturnCode;
import blue.origami.code.OSetIndexCode;
import blue.origami.code.OSetterCode;
import blue.origami.code.OThrowCode;
import blue.origami.code.OTryCode;
import blue.origami.code.OValueCode;
import blue.origami.code.OWhileCode;
import blue.origami.lang.OField;
import blue.origami.rule.OFmt;
import blue.origami.util.OStringUtils;

public class PythonCodeWriter extends SourceCodeWriter implements OGenerator {

	@Override
	public void push(OCode node) {
		node.generate(this);
	}

	public void pushParam(OCode node, String delim) {
		int c = 0;
		for (OCode p : node.getParams()) {
			if (c > 0) {
				this.p(this.s(delim));
				this.pSpace();
			}
			this.push(p);
			c++;
		}
	}

	@Override
	public void pushValue(OValueCode node) {
		Object value = node.getValue();
		if (node.getType().is(void.class)) {
			this.p("pass");
			return;
		}
		if (value == null) {
			this.p(this.s("null", "None"));
			return;
		}
		if (value instanceof Boolean) {
			this.p((Boolean) value ? "True" : "False");
			return;
		}
		if (value instanceof Number) {
			this.p(value);
			return;
		}
		if (value instanceof String) {
			// FIXME
			this.p(OStringUtils.quoteString('"', value.toString(), '"'));
			return;
		}
	}

	@Override
	public void pushArray(OArrayCode node) {
		this.p("[");
		this.pushParam(node, ",");
		this.p("]");
	}

	@Override
	public void pushLambda(OLambdaCode node) {
		String[] names = node.getParamNames();
		node.getFirst();
	}

	@Override
	public void pushName(ONameCode node) {
		this.p(node.getName());
	}

	@Override
	public void pushConstructor(OConstructorCode node) {
		this.pushCons(node.getDeclaringClass(), node.getParams());
	}

	// @Override
	// public void pushMethod(OMethodCode node) {
	// // TODO Auto-generated method stub
	//
	// }

	@Override
	public void pushCast(OCastCode node) {
		if (node.getHandled() != null) {
			this.pushMethod(node);
		} else {
			this.push(node.getFirst());
		}
	}

	@Override
	public void pushSetter(OSetterCode node) {
		OField f = node.getHandled();
		if (f.isStatic()) {
			this.pushSetter(f.getDeclaringClass(), f.getName(), node.getFirst());
		} else {
			this.pushSetter(node.getFirst(), f.getName(), node.getParams()[1]);
		}
	}

	@Override
	public void pushGetter(OGetterCode node) {
		OField f = node.getHandled();
		if (f.isStatic()) {
			this.pushGetter(f.getDeclaringClass(), f.getName());
		} else {
			this.pushGetter(node.getFirst(), f.getName());
		}
	}

	@Override
	public void pushInstanceOf(OInstanceOfCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushAnd(OAndCode code) {
		this.pushBinary(code.getFirst(), "and", code.getParams()[1]);
	}

	@Override
	public void pushOr(OOrCode code) {
		this.pushBinary(code.getFirst(), "or", code.getParams()[1]);
	}

	@Override
	public void pushNot(ONotCode code) {
		this.pushUnary("not", code.getFirst());

	}

	@Override
	public void pushGetSize(OGetSizeCode code) {
		this.pushApply("len", code.getFirst());
	}

	@Override
	public void pushSetIndex(OSetIndexCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGetIndex(OGetIndexCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushMulti(OMultiCode node) {
		for (OCode stmt : node.getParams()) {
			this.L("");
			this.push(stmt);
		}
	}

	@Override
	public void pushAssign(OAssignCode node) {
		this.p(node.getName());
		this.pSpace();
		this.p(this.s("=", "="));
		this.pSpace();
		this.push(node.getFirst());
	}

	@Override
	public void pushIf(OIfCode node) {
		this.p(this.s("if", "if"));
		this.pSpace();
		this.push(node.condCode());
		this.pBegin(":");
		{
			this.push(node.thenCode());
		}
		this.pEnd("");
		if (!(node.elseCode() instanceof OEmptyCode)) {
			this.pBegin("else:");
			this.push(node.elseCode());
			this.pEnd("");
		}
	}

	@Override
	public void pushWhile(OWhileCode code) {
		// if(code.nextCode() instanceof OEmptyCode) {
		//
		// }
		// else {
		this.p(this.s("while", "while"));
		this.pSpace();
		this.push(code.condCode());
		this.pBegin(":");
		{
			this.push(code.bodyCode());
		}
		this.pEnd("");
		// }
	}

	@Override
	public void pushTry(OTryCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushReturn(OReturnCode node) {
		this.p(this.s("return", "return"));
		if (!(node.getFirst() instanceof OEmptyCode)) {
			this.pSpace();
			this.push(node.getFirst());
		}
	}

	@Override
	public void pushThrow(OThrowCode code) {
		this.p(this.s("throw", "raise"));
		this.pSpace();
		this.push(code.getFirst());
	}

	@Override
	public void pushBreak(OBreakCode code) {
		if (code.getLabel() != null) {
			this.reportError(code.getSourcePosition(), OFmt.label_is_unsupported);
		}
		this.p(this.s("break", "break"));
	}

	@Override
	public void pushContinue(OContinueCode code) {
		if (code.getLabel() != null) {
			this.reportError(code.getSourcePosition(), OFmt.label_is_unsupported);
		}
		this.p(this.s("continue", "continue"));
	}

	@Override
	public void pushThis() {
		this.p(this.s("this", "self"));
	}

}
