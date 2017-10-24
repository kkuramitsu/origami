package blue.origami.transpiler.target;

import java.util.Arrays;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
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
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.code.TupleIndexCode;
import blue.origami.transpiler.type.Ty;

public class SourceSection extends SourceBuilder implements CodeSection {

	public SourceSection(SourceSyntaxMapper syntax, SourceTypeMapper ts) {
		super(syntax, ts);
	}

	public void pushFuncDecl(Env env, String name, Ty returnType, String[] paramNames, Ty[] paramTypes, Code code) {
		SourceParams p = new SourceParams(this.syntax, 0, paramNames, paramTypes);
		this.pushIndent("");
		this.pushf(env, this.syntax.fmt("function", "%1$s %2$s(%3$s) {"), returnType, name, p);
		this.pushLine("");
		this.pushBlock(env, code.addReturn());
		this.pushIndentLine(this.syntax.symbol("end function", "end", "}"));
	}

	@Override
	public void pushNone(Env env, NoneCode code) {
		this.push(this.syntax.symbol("null", "null"));
	}

	@Override
	public void pushBool(Env env, BoolCode code) {
		if (code.isTrue()) {
			this.push(this.syntax.symbol("true", "true"));
		} else {
			this.push(this.syntax.symbol("false", "false"));
		}
	}

	@Override
	public void pushInt(Env env, IntCode code) {
		this.pushf(this.syntax.fmt("0:Int", "%d"), code.getValue());
	}

	@Override
	public void pushDouble(Env env, DoubleCode code) {
		this.pushf(this.syntax.fmt("0:Float", "%f"), code.getValue());
	}

	@Override
	public void pushString(Env env, StringCode code) {
		// FIXME
		this.pushf(this.syntax.fmt("0:String", "\"%s\""), code.getValue());
	}

	@Override
	public void pushName(Env env, NameCode code) {
		this.pushf(this.syntax.fmt("varname", "name", "%s"), code.getName());
	}

	@Override
	public void pushLet(Env env, LetCode code) {
		this.pushf(env, this.syntax.fmt("let", "%1$s %2$s=%3$s"), code.getDeclType(), code.getName(), code.getInner());
	}

	@Override
	public void pushCast(Env env, CastCode code) {
		if (code.hasTemplate()) {
			this.pushCall(env, code);
		} else {
			this.pushf(env, this.syntax.fmt("cast", "(%1$s)%2$s"), code.getType(), code.getInner());
		}
	}

	@Override
	public void pushCall(Env env, CallCode code) {
		String fmt = code.getTemplate().getDefined();
		Object[] args = Arrays.stream(code.args()).map(c -> (Object) c).toArray(Object[]::new);
		this.pushf(env, fmt, args);
	}

	@Override
	public void pushIf(Env env, IfCode code) {
		if (code.isStatementStyle()) {
			this.pushf(env, this.syntax.fmt("if", "if(%s) {"), code.condCode());
			this.pushLine("");
			this.pushBlock(env, code.thenCode());
			this.pushIndent(this.syntax.symbol("end if", "end", "}"));
			if (!code.elseCode().isDataType()) {
				this.pushLine(this.syntax.symbol("else", "else {"));
				this.pushBlock(env, code.elseCode());
				this.pushIndent(this.syntax.symbol("end else", "end if", "end", "}"));
			}
		} else {
			this.pushf(env, this.syntax.fmt("ifexpr", "%1$s ? %2$s : %3$"), code.condCode(), code.thenCode(),
					code.elseCode());
		}
	}

	void pushBlock(Env env, Code code) {
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
	public void pushMulti(Env env, MultiCode code) {
		// if (code.isBlockExpr()) {
		// this.pushLine(syntax.symbol("block", "begin", "{"));
		// for (Code c : code) {
		// c.emitCode(env, this);
		// this.push(syntax.symbolOrElse(";", ";"));
		// }
		// this.push(syntax.symbol("end block", "end", "}"));
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
	public void pushReturn(Env env, ReturnCode code) {
		this.pushf(env, this.syntax.fmt("return", "return %1$s;"), code.getInner());
	}

	@Override
	public void pushTemplate(Env env, TemplateCode code) {
		ODebug.TODO();
	}

	@Override
	public void pushData(Env env, DataCode code) {
		if (code.isList()) {
			Ty innTy = code.getType().getInnerTy();
			this.pushf(env, this.syntax.fmt("array", "{"), this.ts.box(innTy));
			int c = 0;
			String delim = this.syntax.symbol("delim", ",", ",");
			for (Code e : code) {
				if (c > 0) {
					this.push(delim);
				}
				e.emitCode(env, this);
				c++;
			}
			this.push(this.syntax.symbol("end array", "}"));
			return;
		}

	}

	@Override
	public void pushError(Env env, ErrorCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushFuncExpr(Env env, FuncCode code) {
		SourceParams p = new SourceParams(this.syntax, code.getStartIndex(), code.getParamNames(),
				code.getParamTypes());
		this.pushf(env, this.syntax.fmt("lambda", "(%1$s)->%2$s"), p, code.getInner(), code.getReturnType());
	}

	@Override
	public void pushApply(Env env, ApplyCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushFuncRef(Env env, FuncRefCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGet(Env env, GetCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushSet(Env env, SetCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushExistField(Env env, ExistFieldCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGroup(Env env, GroupCode code) {
		this.pushf(env, this.syntax.fmt("group", "%s"), code.getInner());
	}

	@Override
	public void pushTuple(Env env, TupleCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushTupleIndex(Env env, TupleIndexCode code) {
		// TODO Auto-generated method stub

	}

}

abstract class SourceBuilder implements CodeSection {
	protected final SourceSyntaxMapper syntax;
	protected final SourceTypeMapper ts;;
	StringBuilder sb = new StringBuilder();
	int indent = 0;

	SourceBuilder(SourceSyntaxMapper syntax, SourceTypeMapper ts) {
		this.syntax = syntax;
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

	void push(Env env, Object value) {
		if (value instanceof Code) {
			((Code) value).emitCode(env, this);
		} else if (value instanceof Ty) {
			this.push(((Ty) value).mapType(this.ts));
		} else if (value instanceof SourceEmitter) {
			((SourceEmitter) value).emit(env, (SourceSection) this);
		} else {
			this.push(value.toString());
		}
	}

	void pushf(String format, int start, int end) {
		if (start < end) {
			this.push(format.substring(start, end));
		}
	}

	void pushf(Env env, String format, Object... args) {
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

interface FuncParam {

	public String[] getParamNames();

	public Ty[] getParamTypes();

	public default int getStartIndex() {
		return 0;
	}

	public default String getNameAt(int index) {
		if (getParamNames().length == 0) {
			return String.valueOf((char) ('a' + index));
		}
		return getParamNames()[index] + (this.getStartIndex() + index);
	}

	public default int size() {
		return this.getParamTypes().length;
	}
}

class SourceParams implements FuncParam, SourceEmitter {
	final SourceSyntaxMapper syntax;
	final int startIndex;
	final String[] paramNames;
	final Ty[] paramTypes;

	SourceParams(SourceSyntaxMapper syntax, int startIndex, String[] paramNames, Ty[] paramTypes) {
		this.syntax = syntax;
		this.startIndex = startIndex;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
	}

	SourceParams(SourceSyntaxMapper syntax, Ty[] paramTypes) {
		this(syntax, 0, OArrays.emptyNames, paramTypes);
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
	public void emit(Env env, SourceSection sec) {
		String delim = this.syntax.symbol("paramdelim", ",", ",");
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sec.push(delim);
			}
			sec.pushf(env, this.syntax.fmt("param", "%1$s %2$s"), this.getParamTypes()[i], this.getNameAt(i));
		}
	}

}