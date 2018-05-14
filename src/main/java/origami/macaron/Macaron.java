package origami.macaron;

import java.io.IOException;

import origami.nez2.PEG;
import origami.nez2.ParseTree;
import origami.nez2.Parser;
import origami.tcode.TCode;
import origami.tcode.TEnv;
import origami.tcode.TSourceWriter;
import origami.tcode.TSyntaxRule;

public class Macaron {
	public void transpile(String path) throws IOException {
		PEG peg = new PEG();
		peg.load("/origami/grammar/macaron.opeg");
		Parser p = peg.getParser();
		ParseTree t = p.parseFile(path);
		TEnv env = new TEnv(null);
		TSyntaxRule rules = new TSyntaxRule();
		rules.init(env);
		TCode code = TSyntaxRule.parseCatch(env, path, t);
		code = this.typeCheck(code);
		TSourceWriter w = new TSourceWriter(path);
		// w.perform(env, code);
	}

	TCode typeCheck(TCode c) {
		return c;
	}
}
