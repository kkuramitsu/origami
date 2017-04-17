// package blue.nez.pegasm;
//
// import blue.nez.ast.Symbol;
// import blue.nez.parser.PegAsmInstruction;
// import blue.nez.parser.PegAsmContext;
// import blue.nez.parser.ParserTerminationException;
// import blue.nez.pegasm.PegAsm.AbstractTableInstruction;
//
// public final class ASMSIsa extends AbstractTableInstruction {
// public ASMSIsa(Symbol tableName, PegAsmInstruction next) {
// super(tableName, next);
// }
//
// @Override
// public void visit(PegAsmVisitor v) {
// v.visitSIsa(this);
// }
//
// @Override
// public PegAsmInstruction exec(PegAsmContext<?> sc) throws
// ParserTerminationException {
// int ppos = sc.xPPos();
// return sc.contains(this.label, ppos) ? this.next : sc.xFail();
// }
//
// }