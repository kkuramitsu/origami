package blue.nez.parser.pasm;
// package blue.nez.pegasm;
//
// import blue.nez.ast.Symbol;
// import blue.nez.parser.ParserContext.SymbolPredicate;
// import blue.nez.parser.ParserTerminationException;
// import blue.nez.parser.PegAsmContext;
// import blue.nez.parser.PegAsmInstruction;
// import blue.nez.pegasm.PegAsm.AbstractTableInstruction;
//
// public final class ASMSexists extends AbstractTableInstruction {
// SymbolPredicate pred;
//
// public ASMSexists(Symbol tableName, PegAsmInstruction next) {
// super(tableName, next);
// }
//
// @Override
// public void visit(PegAsmVisitor v) {
// v.visitSExists(this);
// }
//
// @Override
// public PegAsmInstruction exec(PegAsmContext<?> sc) throws
// ParserTerminationException {
// return pred.match(sc, this.label, 0, 0, null);
// return sc.exists(this.label) ? this.next : sc.xFail();
// }
//
// }