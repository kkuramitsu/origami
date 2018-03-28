package origami.libnez;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class Parser {
	final ParseFunc start;
	final HashMap<String, ParseFunc> funcMap;
	final int memoSize;

	Parser(ParseFunc start, HashMap<String, ParseFunc> funcMap, int memoSize) {
		this.start = start;
		this.funcMap = funcMap;
		this.memoSize = memoSize;
	}

	private ParserContext px(byte[] b, int offset, int len) {
		ParserContext px = new ParserContext();
		px.inputs = b;
		px.pos = offset;
		px.headpos = offset;
		px.length = len;
		px.memos = Arrays.stream(new MemoEntry[this.memoSize]).map(m -> {
			return new MemoEntry();
		}).toArray(MemoEntry[]::new);
		return px;
	}

	public ParseTree parse(byte[] b, int offset, int len) {
		ParserContext px = this.px(b, 0, b.length - 1);
		if (this.start.apply(px)) {
			if (px.tree == null) {
				return new ParseTree(ParseTree.EmptyTag, px.inputs, 0, px.pos, null);
			}
			// this.funcMap.forEach((n, f) -> {
			// if (f instanceof MemoPoint) {
			// System.err.println("MemoPoint " + f);
			// }
			// });
			return (ParseTree) px.tree;
		}
		return new ParseTree(ParseTree.ErrorTag, px.inputs, 0, px.headpos, null);
	}

	public ParseTree parse(byte[] buf) {
		return this.parse(buf, 0, buf.length - 1);
	}

	public ParseTree parse(String text) {
		byte[] b = Loader.encode(text + "\0");
		return this.parse(b, 0, b.length - 1);
	}

	public ParseTree parseFile(String path) throws IOException {
		File file = new File(path);
		byte[] buf = new byte[((int) file.length()) + 1]; // adding '\0' termination
		FileInputStream fin = new FileInputStream(file);
		try {
			fin.read(buf, 0, (int) file.length());
		} finally {
			fin.close();
		}
		return this.parse(buf, 0, buf.length - 1);
	}
}

class MemoEntry {
	long key;
	boolean matched;
	int mpos;
	T mtree;
	State mstate;
}

class State {
	int ns;
	int spos;
	int slen;
	State sprev;

	State(int ns, int spos, int epos, State sprev) {
		this.ns = ns;
		this.spos = spos;
		this.slen = epos - spos;
		this.sprev = sprev;
	}
}

class ParserContext {
	byte[] inputs;
	int length;
	int pos;
	int headpos;
	T tree;
	State state;
	MemoEntry[] memos;
	ParseFunc[] args;

	ParseFunc get(int index) {
		return this.args[index];
	}

	ParseFunc[] push(ParseFunc[] a) {
		ParseFunc[] a0 = this.args;
		this.args = a;
		return a0;
	}

	boolean pop(ParseFunc[] a, boolean r) {
		this.args = a;
		return r;
	}

}
