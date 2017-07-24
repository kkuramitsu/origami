package blue.origami.transpiler;

import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TIfCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TLetCode;
import blue.origami.transpiler.code.TMultiCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TReturnCode;
import blue.origami.util.OLog;

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

	public void pushLine(String line) {
		this.sb.append(this.Indent("  ", line + "\n"));
	}

	@Override
	public void push(String t) {
		this.sb.append(t);
	}

	@Override
	public void push(TCode t) {
		// TODO Auto-generated method stub
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

	@Override
	public void pushLog(OLog log) {
		System.out.println(log);
	}

	// Asm compatible

	@Override
	public void pushBool(TEnv env, TBoolCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushInt(TEnv env, TIntCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushDouble(TEnv env, TDoubleCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushCast(TEnv env, TCastCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushName(TEnv env, TNameCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushLet(TEnv env, TLetCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushCall(TEnv env, TCode code) {
		this.push(code.strOut(env));
	}

	@Override
	public void pushIf(TEnv env, TIfCode code) {
		if (code.isStatementStyle()) {
			String cond = code.condCode().strOut(env);
			this.pushLine(env.format("if", "if(%s) {", cond));
			this.incIndent();
			code.thenCode().emitCode(env, this);
			this.decIndent();
			this.pushLine(env.getSymbol("end if", "end", "}"));
			this.pushLine(env.getSymbol("else", "else {"));
			this.incIndent();
			code.elseCode().emitCode(env, this);
			this.decIndent();
			this.pushLine(env.getSymbol("end else ", "end if", "end", "}"));
		} else {
			this.push(code.strOut(env));
		}

	}

	@Override
	public void pushMulti(TEnv env, TMultiCode code) {
		if (code.isBlockExpr()) {
			this.pushLine(env.getSymbol("block", "begin", "{"));
			this.incIndent();
			code.addReturn();
		}
		for (TCode c : code) {
			this.pushLine(c.strOut(env));
		}
		if (code.isBlockExpr()) {
			this.decIndent();
			this.push(env.getSymbol("end block", "end", "}"));
		}
	}

	@Override
	public void pushReturn(TEnv env, TReturnCode code) {
		// TODO Auto-generated method stub

	}

}