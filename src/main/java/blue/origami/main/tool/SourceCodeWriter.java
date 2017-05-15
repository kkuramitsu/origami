package blue.origami.main.tool;

import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.type.OType;
import blue.origami.nez.ast.LocaleFormat;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.ocode.ApplyCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.OCodeWriter;
import blue.origami.ocode.OGenerator;
import blue.origami.ocode.SugarCode;
import blue.origami.ocode.WarningCode;
import blue.origami.util.OArrayUtils;
import blue.origami.util.ODebug;
import blue.origami.util.OLog;

public abstract class SourceCodeWriter extends OCodeWriter implements OGenerator, OArrayUtils {

	@Override
	public void writeCode(OEnv env, OCode code) throws Throwable {
		this.push(code);
	}

	@Override
	public void writeCodeLine(OEnv env, OCode node) throws Throwable {
		this.logList = new ArrayList<>();
		this.writeCode(env, node);
		this.println();
		for (OLog log : this.logList) {
			OLog.report(env, log);
		}
		this.logList = null;
	}

	private ArrayList<OLog> logList = null;

	protected void reportError(SourcePosition s, LocaleFormat fmt, Object... args) {
		this.logList.add(new OLog(s, OLog.Error, fmt, args));
	}

	@Override
	public void pushError(ErrorCode node) {
		this.logList.add(node.getLog());

	}

	@Override
	public void pushWarning(WarningCode node) {
		this.logList.add(node.getLog());
		this.push(node.getFirst());
	}

	@Override
	public void pushSugar(SugarCode code) {
		code.desugar().generate(this);
	}

	@Override
	public void pushMethod(ApplyCode node) {
		OMethodHandle m = node.getHandled();
		if (this.tryCodeTemplate(m, node.getParams())) {
			return;
		}
		int ivc = m.getInvocation();
		switch (ivc) {
		case OMethodHandle.DynamicInvocation: {
			this.pushMethod(node.getFirst(), m.getName(), this.ltrim(node.getParams()));
			break;
		}
		case OMethodHandle.StaticInvocation: {
			this.pushApply(m.getName(), node.getParams());
			break;
		}
		case OMethodHandle.SpecialInvocation: {
			this.pushCons(m.getDeclaringClass(), node.getParams());
			break;
		}
		case OMethodHandle.VirtualInvocation:
		case OMethodHandle.InterfaceInvocation: {
			this.pushMethod(node.getFirst(), m.getName(), this.ltrim(node.getParams()));
			break;
		}
		case OMethodHandle.StaticGetter: {
			this.pushGetter(m.getDeclaringClass(), m.getName());
			break;
		}
		case OMethodHandle.VirtualGetter: {
			this.pushGetter(node.getFirst(), m.getName());
			break;
		}
		case OMethodHandle.StaticSetter: {
			this.pushSetter(m.getDeclaringClass(), m.getName(), node.getFirst());
			break;
		}
		case OMethodHandle.VirtualSetter: {
			this.pushSetter(node.getFirst(), m.getName(), node.getParams()[1]);
			break;
		}
		}
	}

	protected void pushBinary(OCode c, String op, OCode c2) {
		this.p("(");
		this.push(c);
		this.pSpace();
		this.p(this.s(op));
		this.pSpace();
		this.push(c2);
		this.p(")");
	}

	protected void pushUnary(String op, OCode c) {
		this.p("(");
		this.p(this.s(op));
		this.p("(");
		this.push(c);
		this.p("))");
	}

	protected void pushApply(String name, OCode... params) {
		this.p(name);
		this.pushArguments(params);
	}

	protected void pushCons(OType clazz, OCode... params) {
		this.p(clazz);
		this.pushArguments(params);
	}

	protected void pushMethod(OCode callee, String name, OCode... params) {
		this.push(callee);
		this.p(".");
		this.p(name);
		this.pushArguments(params);
	}

	protected void pushArguments(OCode[] params) {
		this.p("(");
		int c = 0;
		for (OCode p : params) {
			if (c > 0) {
				this.p(",");
				this.pSpace();
			}
			this.push(p);
			c++;
		}
		this.p(")");
	}

	protected void pushGetter(OCode callee, String name) {
		this.push(callee);
		this.p(".");
		this.p(name);
	}

	protected void pushGetter(OType clazz, String name) {
		this.p(name);
	}

	protected void pushSetter(OCode callee, String name, OCode right) {
		this.push(callee);
		this.p(".");
		this.p(name);
		this.p(" = ");
		this.push(right);
	}

	protected void pushSetter(OType clazz, String name, OCode right) {
		this.p(name);
		this.p(" = ");
		this.push(right);
	}

	protected boolean tryCodeTemplate(OMethodHandle mh, OCode[] params) {
		String codeTempl = this.getCodeTemplate(mh);
		if (codeTempl != null) {
			this.pushCodeTemplate(codeTempl, params);
			return true;
		}
		return false;
	}

	private String getCodeTemplate(OMethodHandle mh) {
		StringBuilder sb = new StringBuilder();
		sb.append(mh.getLocalName());
		sb.append("(");
		OType[] p = mh.getThisParamTypes();
		for (OType t : p) {
			sb.append(t.typeDesc(1));
		}
		sb.append(")");
		String desc = sb.toString();
		ODebug.trace("desc=%s", desc);
		return this.getCodeTemplate(desc);
	}

	private String getCodeTemplate(String desc) {
		String path = this.getClass().getName();
		try {
			return ResourceBundle.getBundle(path, Locale.ENGLISH).getString(desc);
		} catch (java.util.MissingResourceException ex) {
		}
		return null;
	}

	private void pushCodeTemplate(String codeTempl, OCode[] params) {
		String delim = "@";
		String[] tokens = codeTempl.split("@", -1);
		this.p(tokens[0]);
		for (int i = 1; i < tokens.length; i++) {
			String t = tokens[i];
			if (t.length() > 0 && Character.isDigit(t.charAt(0))) {
				int index = t.charAt(0) - '0';
				this.push(params[index]);
				this.p(t.substring(1));
			} else {
				this.p(delim);
				this.p(t);
			}
		}
	}

}
