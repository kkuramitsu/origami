package blue.origami.peg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class Parser {
	final ParserFunc start;
	final HashMap<String, ParserFunc> funcMap;
	final int memoSize;

	Parser(ParserFunc start, HashMap<String, ParserFunc> funcMap, int memoSize) {
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

	private TreeNode parse(byte[] b, int offset, int len) {
		ParserContext px = this.px(b, 0, b.length - 1);
		if (this.start.apply(px)) {
			if (px.tree == null) {
				return new TreeNode(TreeNode.EmptyTag, px.inputs, 0, px.pos, null);
			}
			return (TreeNode) px.tree;
		}
		return new TreeNode("err*", px.inputs, 0, px.headpos, null);
	}

	public TreeNode parse(String text) {
		byte[] b = (text + "\0").getBytes();
		return this.parse(b, 0, b.length - 1);
	}

	public TreeNode parseFile(String path) throws IOException {
		File file = new File(path);
		if (file.exists()) {
			byte[] buf = new byte[((int) file.length()) + 1]; // adding '\0' termination
			FileInputStream fin = new FileInputStream(file);
			fin.read(buf, 0, (int) file.length());
			fin.close();
			return this.parse(buf, 0, buf.length - 1);
		} else {
			return this.parse(path);
		}
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
	ParserFunc a;

	ParserFunc get() {
		return this.a;
	}

	ParserFunc push(ParserFunc a) {
		ParserFunc a0 = this.a;
		this.a = a;
		return a0;
	}

	boolean pop(ParserFunc a, boolean r) {
		this.a = a;
		return r;
	}

}
