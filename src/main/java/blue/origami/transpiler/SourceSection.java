package blue.origami.transpiler;

import java.util.Arrays;

import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.CallCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.code.ReturnCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.transpiler.code.TemplateCode;
import blue.origami.util.ODebug;

public class SourceSection implements TCodeSection {

	StringBuilder sb = new StringBuilder();
	int indent = 0;

	public SourceSection() {
	}

	// private SourceSection(int indent) {
	// this.indent = indent;
	// }
	//
	// public SourceSection dup() {
	// return new SourceSection(this.indent);
	// }

	void incIndent() {
		this.indent++;
	}

	void decIndent() {
		assert (this.indent > 0);
		this.indent--;
	}

	String Indent(String tab, String stmt) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.indent; i++) {
			sb.append(tab);
		}
		sb.append(stmt);
		return sb.toString();
	}

	public void push(String t) {
		this.sb.append(t);
	}

	public void pushLine(String line) {
		this.sb.append(line + "\n");
	}

	public void pushIndent(String line) {
		this.sb.append(this.Indent("  ", line));
	}

	// public void pushIndentLine(String line) {
	// this.sb.append(this.Indent(" ", line + "\n"));
	// }

	@Override
	public String toString() {
		return this.sb.toString();
	}

	void push(TEnv env, Object value) {
		if (value instanceof Code) {
			((Code) value).emitCode(env, this);
		} else if (value instanceof Ty) {
			this.pushType(env, (Ty) value);
		} else {
			this.push(value.toString());
		}
	}

	void pushType(TEnv env, Ty ty) {
		this.push(ty.toString());
	}

	void push(String format, int start, int end) {
		if (start < end) {
			this.push(format.substring(start, end));
		}
	}

	void push(TEnv env, String format, Object... args) {
		int start = 0;
		int index = 0;
		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (c == '%') {
				this.push(format, start, i);
				c = i + 1 < format.length() ? format.charAt(i) : '%';
				if (c == 's') {
					this.push(env, args[index]);
					index++;
					i++;
				}
				if ('1' <= c && c <= '9') { // %1$s
					this.push(env, args[c - '1']);
					i += 3;
				}
				if (c == '%') {
					this.push("%");
					i++;
				}
				start = i + 1;
			}
		}
		this.push(format, start, format.length());
	}

	void push(String format, Object... args) {
		this.push(String.format(format, args));
	}

	// Asm compatible

	@Override
	public void pushBool(TEnv env, BoolCode code) {
		if (code.isTrue()) {
			this.push(env.getSymbol("true:Bool", "true"));
		} else {
			this.push(env.getSymbol("false:Bool", "false"));
		}
	}

	@Override
	public void pushInt(TEnv env, IntCode code) {
		this.push(env.fmt("0:Int", "%d"), code.getValue());
	}

	@Override
	public void pushDouble(TEnv env, DoubleCode code) {
		this.push(env.fmt("0:Float", "%f"), code.getValue());
	}

	@Override
	public void pushString(TEnv env, StringCode code) {
		// FIXME
		this.push(env.fmt("0:String", "\"%s\""), code.getValue());
	}

	@Override
	public void pushName(TEnv env, NameCode code) {
		this.push(env.fmt("varname", "name", "%s"), code.getName());
	}

	@Override
	public void pushLet(TEnv env, LetCode code) {
		this.push(env, env.fmt("let", "%1$s %2$s=%3$s"), code.getDeclType(), code.getName(), code.getInner());
	}

	@Override
	public void pushCast(TEnv env, CastCode code) {
		if (code.hasTemplate()) {
			this.pushCall(env, code);
		} else {
			this.push(env, env.fmt("cast", "(%1$s)%2$s"), code.getType(), code.getInner());
		}
	}

	@Override
	public void pushCall(TEnv env, CallCode code) {
		String fmt = code.getTemplate().getDefined();
		Object[] args = Arrays.stream(code.args()).map(c -> (Object) c).toArray(Object[]::new);
		this.push(env, fmt, args);
	}

	@Override
	public void pushIf(TEnv env, IfCode code) {
		// if (code.isStatementStyle()) {
		// String cond = code.condCode().strOut(env);
		// this.pushLine(env.format("if", "if(%s) {", cond));
		// this.incIndent();
		// this.pushIndentLine(code.thenCode());
		// this.decIndent();
		// this.pushIndentLine(env.getSymbol("end if", "end", "}"));
		// this.pushIndentLine(env.getSymbol("else", "else {"));
		// this.incIndent();
		// this.pushIndentLine(code.elseCode());
		// this.decIndent();
		// this.pushIndent(env.getSymbol("end else ", "end if", "end", "}"));
		// } else {
		this.push(env, env.fmt("ifexpr", "%1$s ? %2$s : %3$"), code.condCode(), code.thenCode(), code.elseCode());
		// }
	}

	@Override
	public void pushMulti(TEnv env, MultiCode code) {
		if (code.isBlockExpr()) {
			this.pushLine(env.getSymbol("block", "begin", "{"));
			for (Code c : code) {
				c.emitCode(env, this);
				this.push(env.getSymbolOrElse(";", ";"));
			}
			this.push(env.getSymbol("end block", "end", "}"));
		} else {
			int cnt = 0;
			for (Code c : code) {
				if (cnt > 0) {
					this.pushIndent("");
				}
				c.emitCode(env, this);
				cnt++;
				if (cnt != code.size()) {
					this.pushLine("");
				}
			}
		}
	}

	@Override
	public void pushReturn(TEnv env, ReturnCode code) {
		this.push(env, env.fmt("return", "return %1$s;"), code.getInner());
	}

	@Override
	public void pushTemplate(TEnv env, TemplateCode code) {
		ODebug.TODO();
	}

	@Override
	public void pushData(TEnv env, DataCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushError(TEnv env, ErrorCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushFuncExpr(TEnv env, FuncCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushApply(TEnv env, ApplyCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushFuncRef(TEnv env, FuncRefCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGet(TEnv env, GetCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushSet(TEnv env, SetCode code) {
		// TODO Auto-generated method stub

	}

}