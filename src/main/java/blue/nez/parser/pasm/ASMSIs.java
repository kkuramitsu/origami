package blue.nez.parser.pasm;
// package blue.nez.pegasm;
//
// import blue.nez.ast.Symbol;
// import blue.nez.parser.PegAsmInstruction;
// import blue.nez.parser.PegAsmContext;
// import blue.nez.parser.ParserTerminationException;
// import blue.nez.pegasm.PegAsm.AbstractTableInstruction;
//
// public final class ASMSIs extends AbstractTableInstruction {
// public ASMSIs(Symbol tableName, PegAsmInstruction next) {
// super(tableName, next);
// }
//
// @Override
// public void visit(PegAsmVisitor v) {
// v.visitSIs(this);
// }
//
// @Override
// public PegAsmInstruction exec(PegAsmContext<?> sc) throws
// ParserTerminationException {
// int ppos = sc.xPPos();
// return sc.equals(this.label, ppos) ? this.next : sc.xFail();
// }
//
// }