package origami.nez2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import origami.nez2.ParserFuncGenerator.MemoPoint;

public class Parser {
	final ParseFunc start;
	final HashMap<String, ParseFunc> funcMap;
	final int memoSize;

	Parser(ParseFunc start, HashMap<String, ParseFunc> funcMap, int memoSize) {
		this.start = start;
		this.funcMap = funcMap;
		this.memoSize = memoSize;
	}

	private ParserContext px(byte[] b, int spos, int epos) {
		ParserContext px = new ParserContext();
		px.inputs = b;
		px.pos = spos;
		px.headpos = spos;
		px.length = epos;
		px.memos = Arrays.stream(new MemoEntry[this.memoSize]).map(m -> {
			return new MemoEntry();
		}).toArray(MemoEntry[]::new);
		return px;
	}

	ParseTree parse(byte[] b, int spos, int epos) {
		ParserContext px = this.px(b, spos, epos);
		if (this.start.apply(px)) {
			if (px.tree == null) {
				return new ParseTree(ParseTree.EmptyTag, px.inputs, spos, px.pos, null);
			}
			return (ParseTree) px.tree;
		}
		return new ParseTree(ParseTree.ErrorTag, px.inputs, 0, px.headpos, null);
	}

	ParseTree parse(String path, byte[] b, int spos, int epos) throws ParseException {
		ParserContext px = this.px(b, spos, epos);
		if (this.start.apply(px)) {
			if (px.tree == null) {
				return new ParseTree(ParseTree.EmptyTag, px.inputs, spos, px.pos, null);
			}
			return (ParseTree) px.tree;
		}
		throw new ParseException(new Token("", path, px.inputs, px.headpos, px.length));
	}

	public ParseTree parse(String text) {
		byte[] b = Loader.encode(text + "\0");
		return this.parse(b, 0, b.length - 1);
	}

	public ParseTree parse(Token token) throws ParseException {
		byte b = token.inputs[token.epos];
		token.inputs[token.epos] = 0; // ad hoc
		ParseTree t = this.parse(token.path, token.inputs, token.pos, token.epos);
		token.inputs[token.epos] = b;
		return t;
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
		return this.parse(path, buf, 0, buf.length - 1);
	}

	public void dump() {
		this.funcMap.forEach((n, f) -> {
			if (f instanceof MemoPoint) {
				System.out.println("MemoPoint " + f);
			}
		});
	}
}

class ParseException extends IOException {

	private static final long serialVersionUID = 1L;
	Token token;

	public ParseException(Token token) {
		super(token.token);
		this.token = token;
	}

	@Override
	public void printStackTrace() {
		System.out.println("Syntax Error (" + this.token.getPath() + ":" + this.token.linenum() + ")");
		System.out.println(this.token.line());
		System.out.println(this.token.mark('^'));
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
