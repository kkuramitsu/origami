package blue.origami.transpiler.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;

import blue.origami.util.OConsole;
import blue.origami.util.ODebug;

public class AsmClassLoader extends ClassLoader {

	int seq = 0;
	// private final Map<String, OClassDecl> uncompiledMap;

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

	public void set(String cname, byte[] byteCode) {
		this.dump(cname, byteCode);
		this.codeMap.put(cname, byteCode);
	}

	@Override
	protected Class<?> findClass(String cname) throws ClassNotFoundException {
		byte[] byteCode = this.codeMap.remove(cname);
		if (byteCode == null) {
			throw new ClassNotFoundException("not found " + cname);
		}
		// this.dump(cname, byteCode);
		Class<?> c = this.defineClass(cname, byteCode, 0, byteCode.length);
		return c;
	}

	public boolean traceMode = true;
	private String dumpDirectory = null;

	void dump(String className, byte[] byteCode) {
		if (this.traceMode || this.dumpDirectory != null) {
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
				OConsole.beginColor(OConsole.Cyan);
				OConsole.println("[Generated] " + classFileName + " size=" + byteCode.length);
				ProcessBuilder pb = new ProcessBuilder("javap", "-c", "-l", "-p", "-s",
						/*
						 * "-v" ,
						 */classFileName);
				pb.redirectOutput(Redirect.INHERIT);
				Process p = pb.start();
				p.waitFor();
				p.destroy();
				OConsole.endColor();
			} catch (IOException e) {
				ODebug.trace("Cannot dump " + classFileName + " caused by " + e);
			} catch (InterruptedException e) {
				ODebug.traceException(e);
			}
		}
	}

	// // uniquename
	//
	// HashMap<String, String> shortMap = new HashMap<>();
	// HashMap<Character, Integer> countMap = new HashMap<>();
	//
	// public final String shortName(OType t) {
	// String desc = t.typeDesc(0);
	// String shortName = this.shortMap.get(desc);
	// if (shortName == null) {
	// Character firstChar = t.getName().charAt(0);
	// shortName = String.valueOf(firstChar);
	// Integer c = this.countMap.get(firstChar);
	// if (c == null) {
	// this.countMap.put(firstChar, 0);
	// } else {
	// c = c + 1;
	// shortName = c + shortName;
	// this.countMap.put(firstChar, c);
	// }
	// this.shortMap.put(desc, shortName);
	// }
	// return shortName;
	// }
	//
	// public final String uniqueName(String prefix, OType... params) {
	// StringBuilder sb = new StringBuilder();
	// sb.append(prefix);
	// sb.append("$");
	// for (OType t : params) {
	// sb.append(this.shortName(t));
	// }
	// return sb.toString();
	// }
	//
	// public final String uniqueName(String prefix, int id, OType... params) {
	// StringBuilder sb = new StringBuilder();
	// sb.append(prefix);
	// sb.append("$$");
	// sb.append(1000 + id);
	// sb.append("$");
	// for (OType t : params) {
	// sb.append(this.shortName(t));
	// }
	// return sb.toString();
	// }

	// /* message */
	//
	// private ArrayList<String> messageList = new ArrayList<>();
	//
	// public final int getMessageId(String message) {
	// this.messageList.add(message);
	// return this.messageList.size() - 1;
	// }
	//
	// public final String getMessageById(int md) {
	// return this.messageList.get(md);
	// }

}
