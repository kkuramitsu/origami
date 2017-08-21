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
import blue.origami.transpiler.code.ExistFieldCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.GroupCode;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.code.NoneCode;
import blue.origami.transpiler.code.ReturnCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.transpiler.code.TemplateCode;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;

public class SourceSection extends SourceSectionLib implements TCodeSection {

	public SourceSection(SourceType ts) {
		super(ts);
	}

	@Override
	public void pushNone(TEnv env, NoneCode code) {
		this.push(env.getSymbol("null", "null"));
	}

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
		this.pushf(env.fmt("0:Int", "%d"), code.getValue());
	}

	@Override
	public void pushDouble(TEnv env, DoubleCode code) {
		this.pushf(env.fmt("0:Float", "%f"), code.getValue());
	}

	@Override
	public void pushString(TEnv env, StringCode code) {
		// FIXME
		this.pushf(env.fmt("0:String", "\"%s\""), code.getValue());
	}

	@Override
	public void pushName(TEnv env, NameCode code) {
		this.pushf(env.fmt("varname", "name", "%s"), code.getName());
	}

	@Override
	public void pushLet(TEnv env, LetCode code) {
		this.pushf(env, env.fmt("let", "%1$s %2$s=%3$s"), code.getDeclType(), code.getName(), code.getInner());
	}

	@Override
	public void pushCast(TEnv env, CastCode code) {
		if (code.hasTemplate()) {
			this.pushCall(env, code);
		} else {
			this.pushf(env, env.fmt("cast", "(%1$s)%2$s"), code.getType(), code.getInner());
		}
	}

	@Override
	public void pushCall(TEnv env, CallCode code) {
		String fmt = code.getTemplate().getDefined();
		Object[] args = Arrays.stream(code.args()).map(c -> (Object) c).toArray(Object[]::new);
		this.pushf(env, fmt, args);
	}

	@Override
	public void pushIf(TEnv env, IfCode code) {
		if (code.isStatementStyle()) {
			this.pushf(env, env.fmt("if", "if(%s) {"), code.condCode());
			this.pushLine("");
			this.pushBlock(env, code.thenCode());
			this.pushIndent(env.getSymbol("end if", "end", "}"));
			if (!code.elseCode().isDataType()) {
				this.pushLine(env.getSymbol("else", "else {"));
				this.pushBlock(env, code.elseCode());
				this.pushIndent(env.getSymbol("end else", "end if", "end", "}"));
			}
		} else {
			this.pushf(env, env.fmt("ifexpr", "%1$s ? %2$s : %3$"), code.condCode(), code.thenCode(), code.elseCode());
		}
	}

	void pushBlock(TEnv env, Code code) {
		if (code instanceof MultiCode) {
			this.pushMulti(env, (MultiCode) code);
		} else {
			this.incIndent();
			this.pushIndent("");
			code.emitCode(env, this);
			this.pushLine("");
			this.decIndent();
		}
	}

	@Override
	public void pushMulti(TEnv env, MultiCode code) {
		// if (code.isBlockExpr()) {
		// this.pushLine(env.getSymbol("block", "begin", "{"));
		// for (Code c : code) {
		// c.emitCode(env, this);
		// this.push(env.getSymbolOrElse(";", ";"));
		// }
		// this.push(env.getSymbol("end block", "end", "}"));
		// } else {
		this.incIndent();
		for (Code c : code) {
			this.pushIndent("");
			c.emitCode(env, this);
			this.pushLine("");
		}
		this.decIndent();
		// }
	}

	@Override
	public void pushReturn(TEnv env, ReturnCode code) {
		this.pushf(env, env.fmt("return", "return %1$s;"), code.getInner());
	}

	@Override
	public void pushTemplate(TEnv env, TemplateCode code) {
		ODebug.TODO();
	}

	@Override
	public void pushData(TEnv env, DataCode code) {
		if (code.isList()) {
			Ty innTy = code.getType().getInnerTy();
			this.pushf(env, env.fmt("array", "{"), this.ts.box(innTy));
			int c = 0;
			String delim = env.getSymbol("delim", ",", ",");
			for (Code e : code) {
				if (c > 0) {
					this.push(delim);
				}
				e.emitCode(env, this);
				c++;
			}
			this.push(env.getSymbol("end array", "}"));
			return;
		}

	}

	@Override
	public void pushError(TEnv env, ErrorCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushFuncExpr(TEnv env, FuncCode code) {
		Param p = new Param(code.getStartIndex(), code.getParamNames(), code.getParamTypes());
		this.pushf(env, env.fmt("lambda", "(%1$s)->%2$s"), p, code.getInner(), code.getReturnType());
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

	@Override
	public void pushExistField(TEnv env, ExistFieldCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGroup(TEnv env, GroupCode code) {
		this.pushf(env, env.fmt("group", "%s"), code.getInner());
	}

}

abstract class SourceSectionLib implements TCodeSection {
	protected SourceType ts;
	StringBuilder sb = new StringBuilder();
	int indent = 0;

	SourceSectionLib(SourceType ts) {
		this.ts = ts;
	}

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

	void push(String t) {
		this.sb.append(t);
	}

	void pushLine(String line) {
		this.sb.append(line + "\n");
	}

	void pushIndent(String line) {
		this.sb.append(this.Indent("  ", line));
	}

	void pushIndentLine(String line) {
		this.sb.append(this.Indent("  ", line) + "\n");
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

	void push(TEnv env, Object value) {
		if (value instanceof Code) {
			((Code) value).emitCode(env, this);
		} else if (value instanceof Ty) {
			this.push(((Ty) value).mapType(this.ts));
		} else if (value instanceof Emitter) {
			((Emitter) value).emit(env, (SourceSection) this);
		} else {
			this.push(value.toString());
		}
	}

	void pushf(String format, int start, int end) {
		if (start < end) {
			this.push(format.substring(start, end));
		}
	}

	void pushf(TEnv env, String format, Object... args) {
		int start = 0;
		int index = 0;
		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (c == '\t') {
				this.pushf(format, start, i);
				this.pushIndent("");
				start = i + 1;
			} else if (c == '%') {
				this.pushf(format, start, i);
				c = i + 1 < format.length() ? format.charAt(i + 1) : 0;
				if (c == 's') {
					this.push(env, args[index]);
					index++;
					i++;
				} else if (c == '%') {
					this.push("%");
					i++;
				} else if ('1' <= c && c <= '9') { // %1$s
					int n = c - '1';
					if (!(n < args.length)) {
						System.out.printf("n=%s  %d,%s\n", format, n, args.length);
					}
					this.push(env, args[n]);
					i += 3;
				}
				start = i + 1;
			}
		}
		this.pushf(format, start, format.length());
	}

	void pushf(String format, Object... args) {
		this.push(String.format(format, args));
	}
}

class Param implements FuncParam, Emitter {
	final int startIndex;
	final String[] paramNames;
	final Ty[] paramTypes;

	Param(int startIndex, String[] paramNames, Ty[] paramTypes) {
		this.startIndex = startIndex;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
	}

	Param(Ty[] paramTypes) {
		this(0, TArrays.emptyNames, paramTypes);
	}

	@Override
	public int getStartIndex() {
		return this.startIndex;
	}

	@Override
	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public Ty[] getParamTypes() {
		return this.paramTypes;
	}

	@Override
	public void emit(TEnv env, SourceSection sec) {
		String delim = env.getSymbol("paramdelim", ",", ",");
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sec.push(delim);
			}
			sec.pushf(env, env.fmt("param", "%1$s %2$s"), this.getParamTypes()[i], this.getNameAt(i));
		}
	}

}