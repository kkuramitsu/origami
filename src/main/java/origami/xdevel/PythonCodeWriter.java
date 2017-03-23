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
import origami.code.OErrorCode;
import origami.code.OGenerator;
import origami.code.OGetIndexCode;
import origami.code.OGetSizeCode;
import origami.code.OGetterCode;
import origami.code.OIfCode;
import origami.code.OInstanceOfCode;
import origami.code.OJumpBeforeCode;
import origami.code.OLabelBlockCode;
import origami.code.OMethodCode;
import origami.code.OMultiCode;
import origami.code.ONameCode;
import origami.code.ONotCode;
import origami.code.OOrCode;
import origami.code.OReturnCode;
import origami.code.OSetIndexCode;
import origami.code.OSetterCode;
import origami.code.OSugarCode;
import origami.code.OThrowCode;
import origami.code.OTryCode;
import origami.code.OValueCode;
import origami.code.OWarningCode;
import origami.code.OWhileCode;
import origami.lang.OField;

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
		if (value == null) {
			this.p(this.s("null", "None"));
			return;
		}
		if (value instanceof Boolean) {
			this.p((Boolean) value ? "True" : "False");
		}
		if (value instanceof Number) {
			this.p(value);
		}

	}

	@Override
	public void pushArray(OArrayCode node) {
		this.p("[");
		this.pushParam(node, ",");
		this.p("]");

	}

	@Override
	public void pushName(ONameCode node) {
		this.p(node.getName());
	}

	@Override
	public void pushConstructor(OConstructorCode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushMethod(OMethodCode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushCast(OCastCode node) {
		this.push(node.getFirst());
	}

	@Override
	public void pushSetter(OSetterCode node) {
		OField f = node.getHandled();
		if (f.isStatic()) {
			this.p(f.getName());
			this.pSpace();
			this.p(this.s("=", "="));
			this.pSpace();
			this.push(node.getFirst());
		} else {
			this.push(node.getFirst());
			this.p(this.s("."));
			this.p(f.getName());
			this.pSpace();
			this.p(this.s("=", "="));
			this.pSpace();
			this.push(node.getParams()[1]);
		}
	}

	@Override
	public void pushGetter(OGetterCode node) {
		OField f = node.getHandled();
		if (f.isStatic()) {
			this.p(f.getName());
		} else {
			this.push(node.getFirst());
			this.p(this.s("."));
			this.p(f.getName());
		}
	}

	@Override
	public void pushInstanceOf(OInstanceOfCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushAnd(OAndCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushOr(OOrCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushNot(ONotCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGetSize(OGetSizeCode code) {
		// TODO Auto-generated method stub

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

	}

	@Override
	public void pushTry(OTryCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushReturn(OReturnCode node) {
		this.p(this.s("return", "return"));
	}

	@Override
	public void pushThrow(OThrowCode code) {
		this.p(this.s("throw", "raise"));
	}

	@Override
	public void pushBreak(OBreakCode code) {
		this.p(this.s("break", "break"));
	}

	@Override
	public void pushContinue(OContinueCode code) {
		this.p(this.s("continue", "continue"));
	}

	@Override
	public void pushBlockCode(OLabelBlockCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushThis() {
		this.p(this.s("this", "self"));
	}

	@Override
	public void pushSugar(OSugarCode oSugarCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushError(OErrorCode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushWarning(OWarningCode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushJumpBefore(OJumpBeforeCode code) {
		// TODO Auto-generated method stub

	}

}
