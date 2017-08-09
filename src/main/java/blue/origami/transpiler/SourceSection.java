package blue.origami.transpiler;

import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.BoolCode;
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

public class SourceSection implements TCodeSection {

	StringBuilder sb = new StringBuilder();
	int indent = 0;

	public SourceSection() {
		this(0);
	}

	private SourceSection(int indent) {
		this.indent = indent;
	}

	public SourceSection dup() {
		return new SourceSection(this.indent);
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

	@Override
	public void push(String t) {
		this.sb.append(t);
	}

	public void pushLine(String line) {
		this.sb.append(line + "\n");
	}

	public void pushIndent(String line) {
		this.sb.append(this.Indent("  ", line));
	}

	public void pushIndentLine(String line) {
		this.sb.append(this.Indent("  ", line + "\n"));
	}

	@Override
	public void push(Code t) {
		// TODO Auto-generated method stub
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

	// Asm compatible

	@Override
	public void pushBool(TEnv env, BoolCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushInt(TEnv env, IntCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushDouble(TEnv env, DoubleCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushString(TEnv env, StringCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushCast(TEnv env, CastCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushName(TEnv env, NameCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushLet(TEnv env, LetCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushCall(TEnv env, Code code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushIf(TEnv env, IfCode code) {
		if (code.isStatementStyle()) {
			String cond = code.condCode().strOut(env);
			this.pushLine(env.format("if", "if(%s) {", cond));
			this.incIndent();
			// code.thenCode().emitCode(env, this);
			this.pushIndentLine(code.thenCode().strOut(env));
			this.decIndent();
			this.pushIndentLine(env.getSymbol("end if", "end", "}"));
			this.pushIndentLine(env.getSymbol("else", "else {"));
			this.incIndent();
			this.pushIndentLine(code.elseCode().strOut(env));
			this.decIndent();
			this.pushIndent(env.getSymbol("end else ", "end if", "end", "}"));
		} else {
			this.push(code.strOut(env));
		}
	}

	@Override
	public void pushMulti(TEnv env, MultiCode code) {
		if (code.isBlockExpr()) {
			this.pushLine(env.getSymbol("block", "begin", "{"));
			this.incIndent();
			code.addReturn();
			for (Code c : code) {
				this.pushIndentLine(c.strOut(env));
			}
			this.decIndent();
			this.push(env.getSymbol("end block", "end", "}"));
		} else {
			int cnt = 0;
			for (Code c : code) {
				if (cnt == 0) {
					this.push(c.strOut(env));
				} else {
					this.pushIndent(c.strOut(env));
				}
				cnt++;
				if (cnt != code.size()) {
					this.push("\n");
				}
			}
		}
	}

	@Override
	public void pushReturn(TEnv env, ReturnCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushTemplate(TEnv env, TemplateCode code) {
		// TODO Auto-generated method stub

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