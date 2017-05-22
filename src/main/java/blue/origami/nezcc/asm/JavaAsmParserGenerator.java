package blue.origami.nezcc.asm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import blue.origami.OrigamiContext;
import blue.origami.lang.OClassDecl;
import blue.origami.lang.OEnv;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.peg.expression.ByteSet;
import blue.origami.nezcc.Block;
import blue.origami.nezcc.ParserGenerator;
import blue.origami.ocode.MultiCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.OCodeFactory;

public class JavaAsmParserGenerator extends ParserGenerator<List<OCode>, OCode> implements OCodeFactory {

	private OrigamiContext env;

	@Override
	public OEnv env() {
		return this.env;
	}

	@Override
	protected void initSymbols() {
		this.env = new OrigamiContext();
	}

	@Override
	protected void writeHeader() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeFooter() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declStruct(String typeName, String... fields) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declConst(String typeName, String constName, int arraySize, String literal) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declProtoType(String ret, String funcName, String[] params) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declFunc(String ret, String funcName, String[] params, Block<OCode> block) {
		OClassDecl cdecl = this.env.getClassLoader().currentClassDecl(this.env);
		OCode body = block.block();
		// cdecl.addMethod(anno, ret, name, paramNames, paramTypes, exceptions,
		// body);
	}

	@Override
	protected OCode emitParams(String... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OCode> beginBlock() {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	@Override
	protected void emitStmt(List<OCode> block, OCode expr) {
		block.add(expr);
	}

	@Override
	protected OCode endBlock(List<OCode> block) {
		return new MultiCode(block);
	}

	@Override
	protected OCode emitVarDecl(boolean mutable, String name, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitIfStmt(OCode expr, boolean elseIf, Block<OCode> stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitWhileStmt(OCode expr, Block<OCode> stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitAssign(String name, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitAssign2(OCode left, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitReturn(OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitOp(OCode expr, String op, OCode expr2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitCast(String var, OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitNull(String name) {
		// return new();
		return null;
	}

	@Override
	protected OCode emitArrayIndex(OCode a, OCode index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitNewArray(String type, OCode index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitArrayLength(OCode a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitChar(int uchar) {
		return null;
	}

	@Override
	protected OCode emitGetter(OCode self, String name) {
		return self.newGetterCode(this.env, name);
	}

	@Override
	protected OCode emitSetter(OCode self, String name, OCode expr) {
		return self.newGetterCode(this.env, name);
	}

	@Override
	protected OCode emitFunc(String func, List<OCode> params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitApply(OCode func, List<OCode> params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitNot(OCode pe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitSucc() {
		return this.newValueCode(true);
	}

	@Override
	protected OCode emitFail() {
		return this.newValueCode(false);
	}

	@Override
	protected OCode emitAnd(OCode expr, OCode expr2) {
		return this.newAndCode(expr, expr2);
	}

	@Override
	protected OCode emitOr(OCode expr, OCode expr2) {
		return this.newOrCode(expr, expr2);
	}

	@Override
	protected OCode emitIf(OCode pe, OCode pe2, OCode pe3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitDispatch(OCode index, List<OCode> cases) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitAsm(String expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vInt(int shift) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vIndexMap(byte[] indexMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vString(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vValue(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vTag(Symbol s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vLabel(Symbol s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vThunk(Object s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode vByteSet(ByteSet bs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitFuncRef(String funcName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitParserLambda(OCode match) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OCode V(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OCode Const(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected OCode emitUnsigned(OCode expr) {
		// TODO Auto-generated method stub
		return null;
	}

}
