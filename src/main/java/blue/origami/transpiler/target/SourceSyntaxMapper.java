package blue.origami.transpiler.target;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import blue.origami.common.OConsole;
import blue.origami.parser.nezcc.SourceGenerator;

public class SourceSyntaxMapper {
	HashMap<String, String> syntaxMap = new HashMap<>();

	protected String s(String key) {
		return this.syntaxMap.getOrDefault(key, key);
	}

	public String symbol(String... keys) {
		return this.fmt(keys);
	}

	public String fmt(String... keys) {
		for (int i = 0; i < keys.length - 1; i++) {
			String fmt = this.syntaxMap.get(keys[i]);
			if (fmt != null) {
				return fmt;
			}
		}
		return keys[keys.length - 1];
	}

	protected boolean isDefinedSyntax(String key) {
		return this.syntaxMap.containsKey(key);
	}

	protected String format(String fmt, Object... args) {
		if (args.length == 0) {
			return fmt;
		}
		try {
			return String.format(fmt, args);
		} catch (Exception e) {
			return "FIXME(" + e + ")";
		}
	}

	void defineSyntax(String key, String symbol) {
		if (!this.isDefinedSyntax(key)) {
			if (symbol != null) {
				int s = symbol.indexOf("$|");
				while (s >= 0) {
					int e = symbol.indexOf('|', s + 2);
					String skey = symbol.substring(s + 2, e);
					// if (this.symbolMap.get(skey) != null) {
					symbol = symbol.replace("$|" + skey + "|", this.s(skey));
					// }
					e = s;
					s = symbol.indexOf("$|");
					if (e == s) {
						break; // avoid infinite looping
					}
					// System.out.printf("'%s': %s\n", key, symbol);
				}
			}
			this.syntaxMap.put(key, symbol);
		}
	}

	void importSyntaxFile(String path) {
		try {
			File f = new File(path);
			InputStream s = f.isFile() ? new FileInputStream(path) : SourceGenerator.class.getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(s));
			String line = null;
			String name = null;
			String delim = null;
			StringBuilder text = null;
			while ((line = reader.readLine()) != null) {
				if (text == null) {
					if (line.startsWith("#")) {
						continue;
					}
					int loc = line.indexOf('=');
					if (loc <= 0) {
						continue;
					}
					name = line.substring(0, loc - 1).trim();
					String value = line.substring(loc + 1).trim();
					// System.out.printf("%2$s : %1$s\n", value, name);
					if (value == null) {
						continue;
					}
					if (value.equals("'''") || value.equals("\"\"\"")) {
						delim = value;
						text = new StringBuilder();
					} else {
						this.defineSyntax(name, value);
					}
				} else {
					if (line.trim().equals(delim)) {
						this.defineSyntax(name, text.toString());
						text = null;
					} else {
						if (text.length() > 0) {
							text.append("\n");
						}
						text.append(line);
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			OConsole.exit(1, e);
		}
	}

	/* Properties */

	protected boolean isDyLang;

	public void initProperties() {
		this.isDyLang = this.syntaxMap.get("Int") == null;
	}

}
