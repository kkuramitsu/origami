package blue.origami.xdevel;

import blue.origami.lang.OField;
import blue.origami.ocode.AndCode;
import blue.origami.ocode.ArrayCode;
import blue.origami.ocode.AssignCode;
import blue.origami.ocode.BreakCode;
import blue.origami.ocode.CastCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.NewCode;
import blue.origami.ocode.ContinueCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.ocode.OGenerator;
import blue.origami.ocode.GetIndexCode;
import blue.origami.ocode.GetSizeCode;
import blue.origami.ocode.GetterCode;
import blue.origami.ocode.IfCode;
import blue.origami.ocode.InstanceOfCode;
import blue.origami.ocode.LambdaCode;
import blue.origami.ocode.MultiCode;
import blue.origami.ocode.NameCode;
import blue.origami.ocode.NotCode;
import blue.origami.ocode.OrCode;
import blue.origami.ocode.ReturnCode;
import blue.origami.ocode.SetIndexCode;
import blue.origami.ocode.SetterCode;
import blue.origami.ocode.ThrowCode;
import blue.origami.ocode.TryCode;
import blue.origami.ocode.ValueCode;
import blue.origami.ocode.WhileCode;
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
	public void pushValue(ValueCode node) {
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
	public void pushArray(ArrayCode node) {
		this.p("[");
		this.pushParam(node, ",");
		this.p("]");
	}

	@Override
	public void pushLambda(LambdaCode node) {
		String[] names = node.getParamNames();
		node.getFirst();
	}

	@Override
	public void pushName(NameCode node) {
		this.p(node.getName());
	}

	@Override
	public void pushConstructor(NewCode node) {
		this.pushCons(node.getDeclaringClass(), node.getParams());
	}

	// @Override
	// public void pushMethod(OMethodCode node) {
	// // TODO Auto-generated method stub
	//
	// }

	@Override
	public void pushCast(CastCode node) {
		if (node.getHandled() != null) {
			this.pushMethod(node);
		} else {
			this.push(node.getFirst());
		}
	}

	@Override
	public void pushSetter(SetterCode node) {
		OField f = node.getHandled();
		if (f.isStatic()) {
			this.pushSetter(f.getDeclaringClass(), f.getName(), node.getFirst());
		} else {
			this.pushSetter(node.getFirst(), f.getName(), node.getParams()[1]);
		}
	}

	@Override
	public void pushGetter(GetterCode node) {
		OField f = node.getHandled();
		if (f.isStatic()) {
			this.pushGetter(f.getDeclaringClass(), f.getName());
		} else {
			this.pushGetter(node.getFirst(), f.getName());
		}
	}

	@Override
	public void pushInstanceOf(InstanceOfCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushAnd(AndCode code) {
		this.pushBinary(code.getFirst(), "and", code.getParams()[1]);
	}

	@Override
	public void pushOr(OrCode code) {
		this.pushBinary(code.getFirst(), "or", code.getParams()[1]);
	}

	@Override
	public void pushNot(NotCode code) {
		this.pushUnary("not", code.getFirst());

	}

	@Override
	public void pushGetSize(GetSizeCode code) {
		this.pushApply("len", code.getFirst());
	}

	@Override
	public void pushSetIndex(SetIndexCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGetIndex(GetIndexCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushMulti(MultiCode node) {
		for (OCode stmt : node.getParams()) {
			this.L("");
			this.push(stmt);
		}
	}

	@Override
	public void pushAssign(AssignCode node) {
		this.p(node.getName());
		this.pSpace();
		this.p(this.s("=", "="));
		this.pSpace();
		this.push(node.getFirst());
	}

	@Override
	public void pushIf(IfCode node) {
		this.p(this.s("if", "if"));
		this.pSpace();
		this.push(node.condCode());
		this.pBegin(":");
		{
			this.push(node.thenCode());
		}
		this.pEnd("");
		if (!(node.elseCode() instanceof EmptyCode)) {
			this.pBegin("else:");
			this.push(node.elseCode());
			this.pEnd("");
		}
	}

	@Override
	public void pushWhile(WhileCode code) {
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
	public void pushTry(TryCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushReturn(ReturnCode node) {
		this.p(this.s("return", "return"));
		if (!(node.getFirst() instanceof EmptyCode)) {
			this.pSpace();
			this.push(node.getFirst());
		}
	}

	@Override
	public void pushThrow(ThrowCode code) {
		this.p(this.s("throw", "raise"));
		this.pSpace();
		this.push(code.getFirst());
	}

	@Override
	public void pushBreak(BreakCode code) {
		if (code.getLabel() != null) {
			this.reportError(code.getSourcePosition(), OFmt.label_is_unsupported);
		}
		this.p(this.s("break", "break"));
	}

	@Override
	public void pushContinue(ContinueCode code) {
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
