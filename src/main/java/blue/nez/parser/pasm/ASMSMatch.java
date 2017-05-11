package blue.nez.parser.pasm;
// package blue.nez.pegasm;
//
// import blue.nez.ast.Symbol;
// import blue.nez.parser.PegAsmInstruction;
// import blue.nez.parser.PegAsmContext;
// import blue.nez.parser.ParserTerminationException;
// import blue.nez.pegasm.PegAsm.AbstractTableInstruction;
//
// public final class ASMSMatch extends AbstractTableInstruction {
// public ASMSMatch(Symbol tableName, PegAsmInstruction next) {
// super(tableName, next);
// }
//
// @Override
// public void visit(PegAsmVisitor v) {
// v.visitSMatch(this);
// }
//
// @Override
// public PegAsmInstruction exec(PegAsmContext<?> sc) throws
// ParserTerminationException {
// return sc.matchSymbol(this.label) ? this.next : sc.xFail();
// }
//
// }