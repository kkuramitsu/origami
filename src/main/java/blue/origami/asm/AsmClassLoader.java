package blue.origami.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;

import blue.origami.transpiler.TFmt;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;

public class AsmClassLoader extends ClassLoader {

	int seq = 0;

	public AsmClassLoader() {
		super();
		this.dumpDirectory = System.getenv("DUMPDIR");
	}

	int seq() {
		return this.seq++;
	}

	final static String toClassName(String pathName) {
		return pathName.replace('/', '.');
	}

	final static String toPathName(String className) {
		return className.replace('.', '/');
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> foundClass = this.findLoadedClass(name);
		if (foundClass == null) {
			ClassLoader parent = this.getParent();
			try {
				foundClass = parent.loadClass(name);
			} catch (Throwable e) {
			}
		}
		if (foundClass == null) {
			foundClass = this.findClass(name);
		}
		if (resolve) {
			this.resolveClass(foundClass);
		}
		return foundClass;
	}

	private HashMap<String, byte[]> codeMap = new HashMap<>();

	public void store(String cname, byte[] byteCode) {
		this.dump(cname, byteCode);
		this.codeMap.put(cname, byteCode);
	}

	@Override
	protected Class<?> findClass(String cname) throws ClassNotFoundException {
		byte[] byteCode = this.codeMap.remove(cname);
		if (byteCode == null) {
			throw new ClassNotFoundException("not found '" + cname + "'");
		}
		// this.dump(cname, byteCode);
		Class<?> c = this.defineClass(cname, byteCode, 0, byteCode.length);
		return c;
	}

	private String dumpDirectory = null;

	void dump(String className, byte[] byteCode) {
		ODebug.showCyan(TFmt.GeneratedByteCode.toString(), () -> {
			int index = className.lastIndexOf('.');
			String classFileName = className.substring(index + 1) + ".class";
			if (this.dumpDirectory != null) {
				classFileName = this.dumpDirectory + "/" + classFileName;
			}
			try (FileOutputStream stream = new FileOutputStream(classFileName)) {
				stream.write(byteCode);
				stream.close();
				if (this.dumpDirectory == null) {
					new File(classFileName).deleteOnExit();
				}
				// OConsole.println("[Generated] " + classFileName + " size=" +
				// byteCode.length);
				ProcessBuilder pb = new ProcessBuilder("javap", "-c", "-l", "-p", "-s",
						/*
						 * "-v" ,
						 */classFileName);
				pb.redirectOutput(Redirect.INHERIT);
				Process p = pb.start();
				p.waitFor();
				p.destroy();
			} catch (IOException e) {
				OConsole.println("Cannot dump " + classFileName + " caused by " + e);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

}
