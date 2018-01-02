package blue.origami.transpiler.target;

import java.util.Arrays;

import blue.origami.common.ODebug;
import blue.origami.common.OStringUtils;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.AssignCode;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.BreakCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CharCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.DictCode;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.GroupCode;
import blue.origami.transpiler.code.HasCode;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.ListCode;
import blue.origami.transpiler.code.MappedCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.RangeCode;
import blue.origami.transpiler.code.ReturnCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.transpiler.code.SwitchCode;
import blue.origami.transpiler.code.TemplateCode;
import blue.origami.transpiler.code.ThrowCode;
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.code.TupleIndexCode;
import blue.origami.transpiler.code.VarNameCode;
import blue.origami.transpiler.code.WhileCode;
import blue.origami.transpiler.type.Ty;

public class SourceSection extends SourceBuilder implements CodeSection {

	public SourceSection(SyntaxMapper syntax, SourceTypeMapper ts) {
		super(syntax, ts);
	}

	@Override
	public Env env() {
		return this.ts.env();
	}

	public void pushFuncDecl(String name, Ty returnType, String[] paramNames, Ty[] paramTypes, Code code) {
		SourceParamCode p = new SourceParamCode(this.syntax, 0, paramNames, paramTypes);
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
	public void pushNone(Code code) {
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
		this.push(code.getValue().toString());
	}

	@Override
	public void pushDouble(DoubleCode code) {
		this.push(code.getValue().toString());
	}

	@Override
	public void pushString(StringCode code) {
		this.push(OStringUtils.quoteString('"', code.getValue().toString(), '"'));
	}

	@Override
	public void pushChar(CharCode code) {
		this.push("''" + String.valueOf(code.getValue()) + "'");
	}

	@Override
	public void pushMulti(MultiCode code) {
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
	public void pushLet(LetCode code) {
		if (code.isMutable()) {
			if (this.syntax.isDefinedSyntax("let mut")) {
				this.pushf(this.syntax.fmt("let mut", "let", "%1$s %2$s=%3$s"), code.getDeclType(), code.getName(),
						code.getInner());
				return;
			}
			this.env().reportError(code, TFmt.YY1_cannot_be_used_in_YY2, TFmt.let_mut, this.syntax.target());
		}
		this.pushf(this.syntax.fmt("let", "%1$s %2$s=%3$s"), code.getDeclType(), code.getName(), code.getInner());
	}

	@Override
	public void pushName(VarNameCode code) {
		this.pushfmt(this.syntax.fmt("varname", "name", "%s"), code.getName());
	}

	@Override
	public void pushAssign(AssignCode code) {
		if (!this.syntax.isDefinedSyntax("assign")) {
			this.env().reportError(code, TFmt.YY1_cannot_be_used_in_YY2, TFmt.assign, this.syntax.target());
		}
		this.pushf(this.syntax.fmt("assign", "%1$s=%2$s"), code.getName(), code.getInner());
	}

	@Override
	public void pushGroup(GroupCode code) {
		this.pushf(this.syntax.fmt("group", "%s"), code.getInner());
	}

	@Override
	public void pushCast(CastCode code) {
		if (code.hasMapped()) {
			this.pushCall(code);
		} else {
			if (this.syntax.isDefinedSyntax("cast")) {
				this.pushf(this.syntax.fmt("cast", "(%1$s)%2$s"), code.getType(), code.getInner());
			} else {
				code.getInner().emitCode(this);
			}
		}
	}

	@Override
	public void pushCall(MappedCode code) {
		CodeMap cmap = code.getMapped();
		String fmt = (cmap.is(CodeMap.LazyFormat)) ? this.syntax.s(cmap.getDefined()) : cmap.getDefined();
		Object[] args = Arrays.stream(code.args()).map(c -> (Object) c).toArray(Object[]::new);
		this.pushf(fmt, args);
	}

	boolean isStatementStyle(IfCode code) {
		return code.getType().isVoid() || code.hasReturn();
	}

	@Override
	public void pushIf(IfCode code) {
		if (code.isStatementStyle()) {
			this.pushf(this.syntax.fmt("if", "if (%s) {"), code.condCode());
			this.pushLine("");
			this.pushBlock(code.thenCode());
			this.pushIndent(this.syntax.symbol("end if", "end", "}"));
			;
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
	public void pushReturn(ReturnCode code) {
		if (!this.syntax.isDefinedSyntax("return")) {
			this.env().reportError(code, TFmt.YY1_cannot_be_used_in_YY2, TFmt.return_statement, this.syntax.target());
		}
		this.pushf(this.syntax.fmt("return", "return %1$s;"), code.getInner());
	}

	@Override
	public void pushTemplate(TemplateCode code) {
		ODebug.TODO();
	}

	@Override
	public void pushData(DataCode code) {
		Ty innTy = code.getType();
		String cons = code.isMutable() ? "data" : this.syntax.symbol("record", "data");
		String kv = "pair " + cons;
		this.pushEnc(cons, innTy, code.size(), (n) -> {
			this.pushf(this.syntax.fmt(kv, "\"%1$s\": %2$s"), code.getNames()[n], code.args[n]);
		});
	}

	@Override
	public void pushList(ListCode code) {
		Ty innTy = code.getType().getParamType();
		String cons = code.isMutable() ? "array" : this.syntax.symbol("list", "array");
		this.pushEnc(cons, innTy, code.size(), (n) -> code.args[n].emitCode(this));
		return;
	}

	@Override
	public void pushRange(RangeCode code) {
		this.pushf(this.syntax.fmt("range", "range(%1$s,%2$s)"), code.args[0], code.args[1]);
	}

	@Override
	public void pushDict(DictCode code) {
		Ty innTy = code.getType().getParamType();
		String cons = code.isMutable() ? "dict" : this.syntax.symbol("strmap", "dict");
		String kv = "pair " + cons;
		this.pushEnc(cons, innTy, code.size(), (n) -> {
			this.pushf(this.syntax.fmt(kv, "\"%1$s\": %2$s"), code.getNames()[n], code.args[n]);
		});
	}

	@Override
	public void pushError(ErrorCode code) {
		// TODO Auto-generated method stub
		this.env().reportLog(code.getLog());
		this.pushNone(code);
	}

	@Override
	public void pushFuncExpr(FuncCode code) {
		SourceParamCode p = new SourceParamCode(this.syntax, code.getStartIndex(), AST.names(code.getParamNames()),
				code.getParamTypes());
		this.pushf(this.syntax.fmt("lambda", "(%1$s)->%2$s"), p, code.getInner(), code.getReturnType());
	}

	@Override
	public void pushApply(ApplyCode code) {
		this.pushEnc("apply", code.args[0], 1, code.size(), (n) -> code.args[n].emitCode(this));
	}

	@Override
	public void pushFuncRef(FuncRefCode code) {
		this.pushf(this.syntax.fmt("funcref", "%1$s"), code.getMapped().getName());
	}

	@Override
	public void pushHas(HasCode code) {
		this.pushf(this.syntax.fmt("has", "%2$s in %1$s"), code.getInner(), code.getName());
	}

	@Override
	public void pushGet(GetCode code) {
		this.pushf(this.syntax.fmt("get", "%1$s.%2$s"), code.getInner(), code.getName());
	}

	@Override
	public void pushSet(SetCode code) {
		Code[] a = code.args();
		this.pushf(this.syntax.fmt("set", "%1$s.%2$s = %3$s"), a[0], code.getName(), a[1]);
	}

	@Override
	public void pushTuple(TupleCode code) {
		Ty innTy = code.getType();
		this.pushEnc("tuple", innTy, code.size(), (n) -> code.args[n].emitCode(this));
		return;
	}

	@Override
	public void pushTupleIndex(TupleIndexCode code) {
		if (this.syntax.isDefinedSyntax("gettuple")) {
			this.pushf(this.syntax.fmt("gettuple", "%1$s._%2$s"), code.getInner(), code.getIndex());
		} else {
			this.pushf(this.syntax.fmt("get" + code.getIndex(), "%s"), code.getInner());
		}
	}

	/* Imperative Programming */

	@Override
	public void pushWhile(WhileCode code) {
		if (!this.syntax.isDefinedSyntax("while")) {
			this.env().reportError(code, TFmt.YY1_cannot_be_used_in_YY2, TFmt.while_loop, this.syntax.target());
		}
		this.pushf(this.syntax.fmt("while", "while (%s) {"), code.condCode());
		this.pushLine("");
		this.pushBlock(code.bodyCode());
		this.pushIndent(this.syntax.symbol("end while", "end", "}"));
	}

	@Override
	public void pushBreak(BreakCode code) {
		if (!this.syntax.isDefinedSyntax("break")) {
			this.env().reportError(code, TFmt.YY1_cannot_be_used_in_YY2, TFmt.break_statement, this.syntax.target());
		}
		this.push(this.syntax.s("break"));
	}

	@Override
	public void pushThrow(ThrowCode code) {
		this.pushf(this.syntax.fmt("throw", "throw %s"), code.getInner());
	}

	@Override
	public void pushSwitch(SwitchCode code) {
		if (!this.syntax.isDefinedSyntax("switch")) {
			this.env().reportError(code, TFmt.YY1_cannot_be_used_in_YY2, TFmt.switch_statement, this.syntax.target());
		}

	}

}
