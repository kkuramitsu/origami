// package blue.origami.transpiler;
//
// import java.io.IOException;
// import java.util.HashMap;
//
// import blue.origami.Version;
// import blue.origami.common.OConsole;
// import blue.origami.common.OSource;
// import blue.origami.parser.Parser;
// import blue.origami.parser.ParserSource;
// import blue.origami.parser.peg.Grammar;
// import blue.origami.parser.peg.SourceGrammar;
// import blue.origami.transpiler.rule.CodeMapDecl;
//
// public class CodeLoader {
// final Transpiler env;
//
// final String common;
// final String base;
// final String defaul;
// final HashMap<String, String> keyMap = new HashMap<>();
//
// private Parser parser = null;
// private CodeMapDecl decl = null;
//
// CodeLoader(Transpiler env) {
// this.env = env;
// String target = env.getTargetName();
// this.base = Version.ResourcePath + "/codemap/" + target + "/";
// this.common = this.base.replace(target, "common");
// this.defaul = this.base.replace(target, "default");
// try {
// Grammar g = SourceGrammar.loadFile(Version.ResourcePath +
// "/grammar/chibi.opeg");
// this.parser = g.newParser("CodeFile");
// this.decl = new CodeMapDecl();
// } catch (IOException e) {
// OConsole.exit(1, e);
// }
// }
//
// public String getPath(String file) {
// return this.base + file;
// }
//
// public void load(String file) {
// try {
// this.load(this.common + file, false);
// } catch (Throwable e) {
// }
// try {
// this.load(this.base + file, false);
// } catch (Throwable e) {
// OConsole.exit(1, e);
// }
// try {
// this.load(this.defaul + file, true);
// } catch (Throwable e) {
// }
// }
//
// private void load(String path, boolean isDefault) throws Throwable {
// OSource s = ParserSource.newFileSource(path, null);
// AST t = (AST) this.parser.parse(s, 0, AST.TreeFunc, AST.TreeFunc);
// this.decl.parseCodeMap(this.env, t);
// }
//
// }