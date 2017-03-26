package origami.xdevel;

import origami.code.OAndCode;
import origami.code.OArrayCode;
import origami.code.OAssignCode;
import origami.code.OBreakCode;
import origami.code.OCastCode;
import origami.code.OCode;
import origami.code.OConstructorCode;
import origami.code.OContinueCode;
import origami.code.OEmptyCode;
import origami.code.OGenerator;
import origami.code.OGetIndexCode;
import origami.code.OGetSizeCode;
import origami.code.OGetterCode;
import origami.code.OIfCode;
import origami.code.OInstanceOfCode;
import origami.code.OLabelBlockCode;
import origami.code.OLambdaCode;
import origami.code.OMultiCode;
import origami.code.ONameCode;
import origami.code.ONotCode;
import origami.code.OOrCode;
import origami.code.OReturnCode;
import origami.code.OSetIndexCode;
import origami.code.OSetterCode;
import origami.code.OThrowCode;
import origami.code.OTryCode;
import origami.code.OValueCode;
import origami.code.OWhileCode;
import origami.lang.OField;
import origami.rule.OFmt;
import origami.util.OStringUtils;

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

	@Override
	public void pushBlockCode(OLabelBlockCode code) {

	}

}
