package origami.main;

import origami.nez.ast.Tree;

public class TreeWriter extends CommonWriter implements OOption.OptionalFactory<TreeWriter> {

	@Override
	public Class<?> entryClass() {
		return TreeWriter.class;
	}

	@Override
	public TreeWriter clone() {
		return new TreeWriter();
	}

	public void init(OOption options) {

	}

	public void write(Tree<?> t) {
		print(t.toString());
	}

	public void writeln(Tree<?> t) {
		write(t);
		L();
		flush();
	}

}
