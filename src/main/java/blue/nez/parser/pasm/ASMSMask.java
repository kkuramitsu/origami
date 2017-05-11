package blue.nez.parser.pasm;
// package blue.nez.pegasm;
//
// import blue.nez.ast.Symbol;
// import blue.nez.parser.PegAsmInstruction;
// import blue.nez.parser.PegAsmContext;
// import blue.nez.parser.ParserTerminationException;
// import blue.nez.pegasm.PegAsm.AbstractTableInstruction;
//
// public final class ASMSMask extends AbstractTableInstruction {
// public ASMSMask(Symbol tableName, PegAsmInstruction next) {
// super(tableName, next);
// }
//
// @Override
// public void visit(PegAsmVisitor v) {
// v.visitSMask(this);
// }
//
// @Override
// public PegAsmInstruction exec(PegAsmContext<?> sc) throws
// ParserTerminationException {
// sc.xSOpen();
// sc.addSymbolMask(this.label);
// return this.next;
// }
//
// }