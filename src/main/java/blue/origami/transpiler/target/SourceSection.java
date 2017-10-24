package blue.origami.transpiler.target;

import java.util.Arrays;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.AssignCode;
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

	@Override
	public Env env() {
		return this.ts.env();
	}

	public void pushFuncDecl(String name, Ty returnType, String[] paramNames, Ty[] paramTypes, Code code) {
		SourceParams p = new SourceParams(this.syntax, 0, paramNames, paramTypes);
		this.pushIndent("");
		this.pushf(this.syntax.fmt("function", "%1$s %2$s(%3$s) {"), returnType, name, p);
		this.pushLine("");
		this.pushBlock(code.addReturn());
		this.pushIndentLine(this.syntax.symbol("end function", "end", "}"));
	}

	void pushBlock(Code code) {
		if (code instanceof MultiCode) {
			this.pushMulti((MultiCode) code);
		} else {
			this.incIndent();
			this.pushIndent("");
			code.emitCode(this);
			this.pushLine("");
			this.decIndent();
		}
	}

	@Override
	public void pushNone(NoneCode code) {
		this.push(this.syntax.symbol("null", "null"));
	}

	@Override
	public void pushBool(BoolCode code) {
		if (code.isTrue()) {
			this.push(this.syntax.symbol("true", "true"));
		} else {
			this.push(this.syntax.symbol("false", "false"));
		}
	}

	@Override
	public void pushInt(IntCode code) {
		this.pushf_old(this.syntax.fmt("0:Int", "%d"), code.getValue());
	}

	@Override
	public void pushDouble(DoubleCode code) {
		this.pushf_old(this.syntax.fmt("0:Float", "%f"), code.getValue());
	}

	@Override
	public void pushString(StringCode code) {
		// FIXME
		this.pushf_old(this.syntax.fmt("0:String", "\"%s\""), code.getValue());
	}

	@Override
	public void pushName(NameCode code) {
		this.pushf_old(this.syntax.fmt("varname", "name", "%s"), code.getName());
	}

	@Override
	public void pushLet(LetCode code) {
		this.pushf(this.syntax.fmt("let", "%1$s %2$s=%3$s"), code.getDeclType(), code.getName(), code.getInner());
	}

	@Override
	public void pushCast(CastCode code) {
		if (code.hasTemplate()) {
			this.pushCall(code);
		} else {
			this.pushf(this.syntax.fmt("cast", "(%1$s)%2$s"), code.getType(), code.getInner());
		}
	}

	@Override
	public void pushCall(CallCode code) {
		String fmt = code.getMapped().getDefined();
		Object[] args = Arrays.stream(code.args()).map(c -> (Object) c).toArray(Object[]::new);
		this.pushf(fmt, args);
	}

	@Override
	public void pushIf(IfCode code) {
		if (code.isStatementStyle()) {
			this.pushf(this.syntax.fmt("if", "if(%s) {"), code.condCode());
			this.pushLine("");
			this.pushBlock(code.thenCode());
			this.pushIndent(this.syntax.symbol("end if", "end", "}"));
			if (!code.elseCode().isDataType()) {
				this.pushLine(this.syntax.symbol("else", "else {"));
				this.pushBlock(code.elseCode());
				this.pushIndent(this.syntax.symbol("end else", "end if", "end", "}"));
			}
		} else {
			this.pushf(this.syntax.fmt("ifexpr", "%1$s ? %2$s : %3$"), code.condCode(), code.thenCode(),
					code.elseCode());
		}
	}

	@Override
	public void pushMulti(MultiCode code) {
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
			c.emitCode(this);
			this.pushLine("");
		}
		this.decIndent();
		// }
	}

	@Override
	public void pushReturn(ReturnCode code) {
		this.pushf(this.syntax.fmt("return", "return %1$s;"), code.getInner());
	}

	@Override
	public void pushTemplate(TemplateCode code) {
		ODebug.TODO();
	}

	@Override
	public void pushData(DataCode code) {
		if (code.isList()) {
			Ty innTy = code.getType().getInnerTy();
			this.pushf(this.syntax.fmt("array", "{"), this.ts.box(innTy));
			int c = 0;
			String delim = this.syntax.symbol("delim", ",", ",");
			for (Code e : code) {
				if (c > 0) {
					this.push(delim);
				}
				e.emitCode(this);
				c++;
			}
			this.push(this.syntax.symbol("end array", "}"));
			return;
		}

	}

	@Override
	public void pushError(ErrorCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushFuncExpr(FuncCode code) {
		SourceParams p = new SourceParams(this.syntax, code.getStartIndex(), code.getParamNames(),
				code.getParamTypes());
		this.pushf(this.syntax.fmt("lambda", "(%1$s)->%2$s"), p, code.getInner(), code.getReturnType());
	}

	@Override
	public void pushApply(ApplyCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushFuncRef(FuncRefCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGet(GetCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushSet(SetCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushExistField(ExistFieldCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushGroup(GroupCode code) {
		this.pushf(this.syntax.fmt("group", "%s"), code.getInner());
	}

	@Override
	public void pushTuple(TupleCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushTupleIndex(TupleIndexCode code) {
		// TODO Auto-generated method stub

	}

	/* Imperative Programming */

	@Override
	public void pushAssign(AssignCode code) {
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

	void pushE(Object value) {
		if (value instanceof Code) {
			((Code) value).emitCode(this);
		} else if (value instanceof Ty) {
			this.push(((Ty) value).mapType(this.ts));
		} else if (value instanceof SourceEmitter) {
			((SourceEmitter) value).emit((SourceSection) this);
		} else {
			this.push(value.toString());
		}
	}

	// void pushf(String format, int start, int end) {
	// if (start < end) {
	// this.push(format.substring(start, end));
	// }
	// }

	void pushf(String format, Object... args) {
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
					this.pushE(args[index]);
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
					this.pushE(args[n]);
					i += 3;
				}
				start = i + 1;
			}
		}
		this.pushf(format, start, format.length());
	}

	void pushf_old(String format, Object... args) {
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
	public void emit(SourceSection sec) {
		String delim = this.syntax.symbol("paramdelim", ",", ",");
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sec.push(delim);
			}
			sec.pushf(this.syntax.fmt("param", "%1$s %2$s"), this.getParamTypes()[i], this.getNameAt(i));
		}
	}

}