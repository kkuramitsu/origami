package origami.tcode;

import java.util.ArrayList;

import blue.origami.common.OWriter;

public class TSourceWriter implements TCodeWriter {
	protected TSyntaxMapper syntaxMapper;
	protected OWriter writer;
	protected TSourceSection head;
	protected ArrayList<TSourceSection> secList = new ArrayList<>();

	public TSourceWriter(String path) {
		this.syntaxMapper.importSyntaxFile(path);
		this.writer = new OWriter();
		this.secList = new ArrayList<>();
	}

	// public TSourceSection newSection() {
	// return new TSourceSection(this.syntax, this.ts);
	// }

	public void check(TCode code) {

	}

	public Object emit() {
		// this.writer.println(this.head.toString());
		// this.writer.println(this.data.toString());
		for (TSourceSection sec : this.secList) {
			this.writer.println(sec.toString());
		}
		// String evaled = this.eval.toString();
		// this.writer.println(evaled);
		// this.writer.close();
		// return evaled;
		return null;
	}

}