package blue.origami.chibi;

import java.lang.reflect.Field;

import blue.origami.common.OConsole;
import origami.libnez.OStrings;

public class Data$ implements OStrings, Cloneable {

	@Override
	public Data$ clone() {
		Object d = this.newf();
		Field[] fs = this.getClass().getDeclaredFields();
		for (Field f : fs) {
			this.setf(f, this, d);
		}
		return (Data$) d;
	}

	private Data$ newf() {
		try {
			return this.getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			OConsole.exit(1, e);
			return this;
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("{");
		int cnt = 0;
		Field[] fs = this.getClass().getDeclaredFields();
		for (Field f : fs) {
			if (cnt > 0) {
				sb.append(", ");
			}
			String name = f.getName();
			sb.append(name);
			sb.append(": ");
			OStrings.appendQuoted(sb, this.getf(f, this));
			cnt++;
		}
		sb.append("}");
	}

	private Object getf(Field f, Object o) {
		try {
			return f.get(o);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			OConsole.exit(1, e);
			return null;
		}
	}

	private void setf(Field f, Object o, Object o2) {
		try {
			f.set(o2, f.get(o));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			OConsole.exit(1, e);
		}
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

}
