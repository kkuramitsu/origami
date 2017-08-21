package blue.origami.konoha5;

import java.util.stream.IntStream;

import blue.origami.konoha5.Func.FuncIntVoid;
import blue.origami.util.StringCombinator;

public class Range$Int extends List$Int implements StringCombinator {
	final int start;
	final int until;

	public Range$Int(int start, int until) {
		super(0);
		this.start = start;
		this.until = until;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Range$Int) {
			Range$Int r = (Range$Int) o;
			return this.start == r.start && this.until == r.until;
		}
		return false;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[");
		StringCombinator.append(sb, this.start);
		sb.append(" to ");
		StringCombinator.append(sb, this.until);
		sb.append("]");
	}

	@Override
	public int size() {
		return this.until - this.start + 1;
	}

	@Override
	public int geti(int index) {
		return index - this.start;
	}

	@Override
	public void forEach(FuncIntVoid f) {
		for (int i = this.start; i <= this.until; i++) {
			f.apply(i);
		}
	}

	@Override
	public IntStream stream() {
		return IntStream.rangeClosed(this.start, this.until);
	}

}
