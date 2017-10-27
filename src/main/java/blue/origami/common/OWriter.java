package blue.origami.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;

public class OWriter {
	protected Process process = null;
	protected PrintStream out;
	protected char lastChar = '\n';

	public OWriter() {
		this.out = System.out;
		this.lastChar = '\n';
	}

	public void open(String path) throws IOException {
		this.close();
		if (path != null) {
			OConsole.println("writing %s ...", path);
			if (path.startsWith("|")) {
				String[] com = path.substring(1).trim().split("\\s");
				ProcessBuilder pb = new ProcessBuilder(com);
				// 1ProcessBuilder pb = new ProcessBuilder("python", "-");
				pb.redirectOutput(Redirect.INHERIT);
				this.process = pb.start();
				this.out = new PrintStream(this.process.getOutputStream());
			} else {
				this.out = new PrintStream(new FileOutputStream(path));
			}
		} else {
			this.out = System.out;
		}
		this.lastChar = '\n';
	}

	public final void close() {
		this.println();
		this.out.flush();
		if (this.out != System.out) {
			this.out.close();
		}
		if (this.process != null) {
			try {
				this.process.waitFor();
			} catch (InterruptedException e) {
			}
			this.process.destroy();
			this.process = null;
		}
	}

	public final void flush() {
		this.out.flush();
	}

	public void println(String s) {
		if (this.lastChar != '\n') {
			this.out.println();
		}
		if (s.length() > 0) {
			this.out.println(s);
		}
		this.lastChar = '\n';
	}

	public final void println() {
		if (this.lastChar == '\n') {
			return;
		}
		this.out.println();
		this.lastChar = '\n';
	}

}