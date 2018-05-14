package origami.tcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import blue.origami.common.OConsole;

public class TSyntaxMapper {
	HashMap<String, String> syntaxMap = new HashMap<>();

	public interface TCodeSection {
		public void emit(TCode c);
	}

	public void importSyntaxFile(String path) {
		try {
			File f = new File(path);
			InputStream s = f.isFile() ? new FileInputStream(path) : TSyntaxMapper.class.getResourceAsStream(path);
			if (s == null) {
				throw new FileNotFoundException(path);
			}
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
					int loc = line.indexOf(" = ");
					if (loc <= 0) {
						continue;
					}
					name = line.substring(0, loc - 1).trim();
					if (name.startsWith("`")) {
						name = name.substring(1);
					}
					String value = line.substring(loc + 3).trim();
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

	public boolean isDefined(String key) {
		return this.syntaxMap.containsKey(key);
	}

	public void defineSyntax(String key, String symbol) {
		if (!this.isDefined(key)) {
			if (symbol != null) {
				int s = symbol.indexOf("$|");
				while (s >= 0) {
					int e = symbol.indexOf('|', s + 2);
					String skey = symbol.substring(s + 2, e);
					symbol = symbol.replace("$|" + skey + "|", this.s(skey));
					e = s;
					s = symbol.indexOf("$|");
					if (e == s) {
						break; // avoid infinite looping
					}
				}
				s = symbol.indexOf('\\');
				if (s != 0) {
					symbol = symbol.replace("\\f", "\f");
					symbol = symbol.replace("\\b", "\b");
					symbol = symbol.replace("\\t", "\t");
					symbol = symbol.replace("\\n", "\n");
				}
			}
			this.syntaxMap.put(key, symbol);
		}
	}

	public String s(String key) {
		return this.syntaxMap.getOrDefault(key, key);
	}

	public String get(String key) {
		return this.syntaxMap.get(key);
	}

	public String getOrDefault(String key, String s) {
		return this.syntaxMap.getOrDefault(key, s);
	}

	public String getOrDefault(String key, String key2, String format) {
		if (this.syntaxMap.containsKey(key)) {
			return this.syntaxMap.get(key);
		}
		return this.syntaxMap.getOrDefault(key2, format);
	}

	public String getOrDefault(String key, String key2, String key3, String format) {
		if (this.syntaxMap.containsKey(key)) {
			return this.syntaxMap.get(key);
		}
		if (this.syntaxMap.containsKey(key2)) {
			return this.syntaxMap.get(key2);
		}
		return this.syntaxMap.getOrDefault(key3, format);
	}

}