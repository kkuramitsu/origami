package blue.origami.konoha5;

import blue.origami.konoha5.Func.FuncIntBool;
import blue.origami.konoha5.Func.FuncIntInt;
import blue.origami.konoha5.Func.FuncIntObj;
import blue.origami.konoha5.Func.FuncIntVoid;
import blue.origami.util.StringCombinator;

public class IntRange extends IntArray implements StringCombinator {
	final int start;
	final int until;

	public IntRange(int start, int until) {
		super(0);
		this.start = start;
		this.until = until;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IntRange) {
			IntRange r = (IntRange) o;
			return this.start == r.start && this.until == r.until;
		}
		return false;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("{");
		StringCombinator.append(sb, this.start);
		sb.append(" to ");
		StringCombinator.append(sb, this.until);
		sb.append("}");
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
	public IntArray map(FuncIntInt f) {
		int[] a = new int[this.size()];
		for (int i = this.start; i <= this.until; i++) {
			a[i - this.start] = f.applyI(i);
		}
		return new IntArray(a, a.length);
	}

	@Override
	public ObjArray map(FuncIntObj f) {
		Object[] a = new Object[this.size()];
		for (int i = this.start; i <= this.until; i++) {
			a[i - this.start] = f.apply(i);
		}
		return new ObjArray(a, a.length);
	}

	@Override
	public IntArray filter(FuncIntBool f) {
		int[] a = new int[this.size()];
		int c = 0;
		for (int i = this.start; i <= this.until; i++) {
			if (f.applyZ(i)) {
				a[i - this.start] = i;
			}
		}
		return new IntArray(a, c);
	}

}
